package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Examples;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.GherkinDocument;
import io.cucumber.messages.types.ParseError;
import io.cucumber.messages.types.RuleChild;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.Source;
import io.cucumber.messages.types.SourceMediaType;
import io.cucumber.messages.types.TableCell;
import io.cucumber.messages.types.TableRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Validates BDD/Gherkin source for TC-004. Wraps {@link GherkinParser} so the
 * AST never leaks past this seam — callers receive a structured success/failure
 * signal and the canonical authored source is stored verbatim.
 *
 * <p>Errors carry only line / column / keyword / field metadata; never the
 * source text, examples cell values, parser stack traces, file paths, or
 * tokens. This is the error-envelope guardrail from the architecture preflight
 * (ADR-042).
 *
 * <p>Limits are static for TC-004 — making them configurable would broaden the
 * surface beyond what the requirement asks for. If a downstream requirement
 * needs configurability, bind through validated
 * {@code @ConfigurationProperties} per the preflight.
 */
@Component
public class GherkinValidator {

    public static final int MAX_SOURCE_LENGTH = 102_400;
    public static final int MAX_SCENARIOS = 50;
    public static final int MAX_EXAMPLES_ROWS = 200;
    public static final int MAX_EXAMPLES_CELL_LENGTH = 4_000;

    private static final String CODE = "invalid_gherkin_source";
    private static final String VIRTUAL_URI = "test-case.feature";

    // Detail-map keys (extracted to constants so the error-envelope contract
    // is stable and Sonar's duplicate-literal rule stays quiet).
    private static final String DETAIL_FIELD = "field";
    private static final String DETAIL_LINE = "line";
    private static final String DETAIL_COLUMN = "column";
    private static final String DETAIL_KEYWORD = "keyword";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_EXAMPLES = "examples";
    private static final String FIELD_SCENARIO = "scenario";
    private static final String KEYWORD_EXAMPLES = "Examples";

    // Pickles expand Scenario Outline examples into per-row execution targets
    // before validation runs — pure waste for a validation-only seam. Disable
    // them so the parser stops after producing the GherkinDocument we read.
    private final GherkinParser parser =
            GherkinParser.builder().includePickles(false).build();

    public void validate(String source) {
        if (source == null || source.isBlank()) {
            throw new DomainValidationException(
                    "Gherkin source must not be blank", CODE, Map.of(DETAIL_FIELD, FIELD_SOURCE));
        }
        if (source.length() > MAX_SOURCE_LENGTH) {
            throw new DomainValidationException(
                    "Gherkin source exceeds maximum length",
                    CODE,
                    Map.of(DETAIL_FIELD, FIELD_SOURCE, "maxLength", String.valueOf(MAX_SOURCE_LENGTH)));
        }
        var envelopes = parseEnvelopes(source);
        rejectIfParseErrors(envelopes);
        var document = extractDocument(envelopes);
        var feature = document.getFeature()
                .orElseThrow(() -> new DomainValidationException(
                        "Gherkin source must declare a Feature",
                        CODE,
                        Map.of(DETAIL_FIELD, FIELD_SOURCE, DETAIL_KEYWORD, "Feature")));
        validateFeature(feature);
    }

    private List<Envelope> parseEnvelopes(String source) {
        var envelope = Envelope.of(new Source(VIRTUAL_URI, source, SourceMediaType.TEXT_X_CUCUMBER_GHERKIN_PLAIN));
        return parser.parse(envelope).toList();
    }

    private void rejectIfParseErrors(List<Envelope> envelopes) {
        Optional<ParseError> firstError = envelopes.stream()
                .map(Envelope::getParseError)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        if (firstError.isPresent()) {
            var err = firstError.get();
            Map<String, String> details = new LinkedHashMap<>();
            details.put(DETAIL_FIELD, FIELD_SOURCE);
            err.getSource().getLocation().ifPresent(loc -> {
                details.put(DETAIL_LINE, String.valueOf(loc.getLine()));
                loc.getColumn().ifPresent(col -> details.put(DETAIL_COLUMN, String.valueOf(col)));
            });
            // err.getMessage() carries the parser's diagnostic, which can echo
            // user-controlled tokens from the offending line. The preflight
            // forbids reflecting source content in error envelopes, so we drop
            // it. Line + column + keyword are enough to locate the failure.
            throw new DomainValidationException("Gherkin source is not syntactically valid", CODE, details);
        }
    }

    private GherkinDocument extractDocument(List<Envelope> envelopes) {
        return envelopes.stream()
                .map(Envelope::getGherkinDocument)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new DomainValidationException(
                        "Gherkin source produced no document", CODE, Map.of(DETAIL_FIELD, FIELD_SOURCE)));
    }

    private void validateFeature(Feature feature) {
        // Gherkin allows Scenario / Scenario Outline at the top level AND
        // nested inside Rule blocks. Both flavours count toward the per-feature
        // scenario cap and both must pass the per-scenario validation.
        var scenarios = collectAuthoredScenarios(feature);
        if (scenarios.size() > MAX_SCENARIOS) {
            throw new DomainValidationException(
                    "Gherkin source exceeds maximum scenario count",
                    CODE,
                    Map.of(DETAIL_FIELD, "scenarios", "maxScenarios", String.valueOf(MAX_SCENARIOS)));
        }
        if (scenarios.isEmpty()) {
            throw new DomainValidationException(
                    "Gherkin source must contain at least one Scenario or Scenario Outline",
                    CODE,
                    Map.of(DETAIL_FIELD, FIELD_SOURCE, DETAIL_KEYWORD, "Scenario"));
        }
        for (Scenario scenario : scenarios) {
            validateScenario(scenario);
        }
    }

    private List<Scenario> collectAuthoredScenarios(Feature feature) {
        var scenarios = new ArrayList<Scenario>();
        for (FeatureChild child : feature.getChildren()) {
            child.getScenario().ifPresent(scenarios::add);
            child.getRule().ifPresent(rule -> {
                for (RuleChild ruleChild : rule.getChildren()) {
                    ruleChild.getScenario().ifPresent(scenarios::add);
                }
            });
        }
        return scenarios;
    }

    private void validateScenario(Scenario scenario) {
        // Parser guarantees non-null lists for steps / examples — only check
        // emptiness. (Sonar previously flagged the redundant null-guards.)
        if (scenario.getSteps().isEmpty()) {
            throw new DomainValidationException(
                    "Scenario must contain at least one step",
                    CODE,
                    Map.of(
                            DETAIL_FIELD,
                            FIELD_SCENARIO,
                            DETAIL_LINE,
                            String.valueOf(scenario.getLocation().getLine()),
                            DETAIL_KEYWORD,
                            "Steps"));
        }
        // The Gherkin grammar treats "Scenario" and "Scenario Outline" as the
        // same syntactic node, distinguished only by the keyword text and the
        // presence of Examples. If the author wrote "Scenario Outline" they
        // must supply an Examples block — otherwise the scenario is degenerate
        // (placeholders, if any, would never be substituted). Parser populates
        // keyword for every authored scenario, so no null guard needed.
        if (scenario.getKeyword().trim().equalsIgnoreCase("Scenario Outline")
                && scenario.getExamples().isEmpty()) {
            throw new DomainValidationException(
                    "Scenario Outline must include at least one Examples block",
                    CODE,
                    Map.of(
                            DETAIL_FIELD, FIELD_SCENARIO,
                            DETAIL_LINE, String.valueOf(scenario.getLocation().getLine()),
                            DETAIL_KEYWORD, KEYWORD_EXAMPLES));
        }
        for (Examples examples : scenario.getExamples()) {
            validateExamples(scenario, examples);
        }
    }

    private void validateExamples(Scenario scenario, Examples examples) {
        // Parser returns a non-null body list; only header may be Optional.
        var header = examples.getTableHeader();
        List<TableRow> body = examples.getTableBody();
        if (header.isEmpty() || body.isEmpty()) {
            throw new DomainValidationException(
                    "Examples table must have a header and at least one data row",
                    CODE,
                    Map.of(
                            DETAIL_FIELD, FIELD_EXAMPLES,
                            DETAIL_LINE, String.valueOf(scenario.getLocation().getLine()),
                            DETAIL_KEYWORD, KEYWORD_EXAMPLES));
        }
        if (body.size() > MAX_EXAMPLES_ROWS) {
            throw new DomainValidationException(
                    "Examples table exceeds maximum row count",
                    CODE,
                    Map.of(DETAIL_FIELD, FIELD_EXAMPLES, "maxRows", String.valueOf(MAX_EXAMPLES_ROWS)));
        }
        // The header row holds the parameter names whose identity ADR-042
        // requires to be preserved; the same cell-length cap that applies to
        // body cells must apply here too. Otherwise an authored parameter
        // name could exceed the cap as long as the whole source stayed under
        // MAX_SOURCE_LENGTH.
        validateCells(header.get());
        for (TableRow row : body) {
            validateCells(row);
        }
    }

    private void validateCells(TableRow row) {
        // Parser populates cell value for every TableCell it emits.
        for (TableCell cell : row.getCells()) {
            if (cell.getValue().length() > MAX_EXAMPLES_CELL_LENGTH) {
                throw new DomainValidationException(
                        "Examples cell exceeds maximum length",
                        CODE,
                        Map.of(
                                DETAIL_FIELD,
                                FIELD_EXAMPLES,
                                "maxCellLength",
                                String.valueOf(MAX_EXAMPLES_CELL_LENGTH)));
            }
        }
    }
}
