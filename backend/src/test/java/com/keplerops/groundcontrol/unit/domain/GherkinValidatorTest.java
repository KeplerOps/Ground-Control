package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.testcases.service.GherkinValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Clause-by-clause coverage for TC-004:
 *  - Given/When/Then syntax (step keywords)
 *  - Scenario blocks
 *  - Scenario Outline blocks
 *  - Examples tables for parameterized scenarios
 *
 * Plus the preflight error-envelope guardrail: failures must surface
 * line/column/keyword/field only — never the source text or examples cell
 * values.
 */
class GherkinValidatorTest {

    private final GherkinValidator validator = new GherkinValidator();

    private static final String GIVEN_WHEN_THEN =
            """
            Feature: Sign in

              Scenario: Successful sign-in
                Given the user is on the sign-in page
                When they submit valid credentials
                Then they are redirected to the dashboard
            """;

    private static final String AND_BUT_CONTINUATION =
            """
            Feature: Sign in

              Scenario: Locked account
                Given the user is on the sign-in page
                And their account is locked
                When they submit valid credentials
                But the lock has not expired
                Then they see the locked-account message
            """;

    private static final String SCENARIO_OUTLINE_WITH_EXAMPLES =
            """
            Feature: Sign in

              Scenario Outline: Reject invalid passwords
                Given the user is on the sign-in page
                When they submit "<email>" / "<password>"
                Then they see the message "<message>"

                Examples:
                  | email          | password   | message              |
                  | a@example.com  | short      | password too short   |
                  | b@example.com  |            | password is required |
                  | c@example.com  | wrong-pass | invalid credentials  |
            """;

    private static final String RULE_NESTED_SCENARIO =
            """
            Feature: Sign in

              Rule: every sign-in path

                Scenario: rule-nested scenario
                  Given the user is on the sign-in page
                  When they submit valid credentials
                  Then they are redirected to the dashboard
            """;

    /**
     * Covers TC-004 clauses 1-4: Given/When/Then, And/But continuation, Scenario
     * Outline + Examples, and Rule-nested scenarios all parse and validate
     * without throwing. Per Sonar rule java:S5961, the four originally-separate
     * "accepts" tests are consolidated here — the test name still identifies
     * which clause each input exercises.
     */
    @ParameterizedTest(name = "accepts {0}")
    @CsvSource({
        "given-when-then,GIVEN_WHEN_THEN",
        "and-but-continuation,AND_BUT_CONTINUATION",
        "scenario-outline-with-examples,SCENARIO_OUTLINE_WITH_EXAMPLES",
        "rule-nested-scenario,RULE_NESTED_SCENARIO",
    })
    void acceptsValidGherkinShape(String label, String inputName) {
        String src =
                switch (inputName) {
                    case "GIVEN_WHEN_THEN" -> GIVEN_WHEN_THEN;
                    case "AND_BUT_CONTINUATION" -> AND_BUT_CONTINUATION;
                    case "SCENARIO_OUTLINE_WITH_EXAMPLES" -> SCENARIO_OUTLINE_WITH_EXAMPLES;
                    case "RULE_NESTED_SCENARIO" -> RULE_NESTED_SCENARIO;
                    default -> throw new IllegalArgumentException(inputName);
                };
        assertThatCode(() -> validator.validate(src)).doesNotThrowAnyException();
    }

    @Test
    void rejectsSyntacticallyInvalidGherkin() {
        // Tag without a Feature header is a parser error.
        var src = "Not a feature at all\nGiven I do something\n";

        assertThatThrownBy(() -> validator.validate(src))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex -> assertThat(((DomainValidationException) ex).getErrorCode())
                        .isEqualTo("invalid_gherkin_source"));
    }

    @Test
    void rejectsBlankSource() {
        assertThatThrownBy(() -> validator.validate("")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> validator.validate("   \n  ")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsSourceOverMaxLength() {
        var huge = "Feature: a\nScenario: b\nGiven c\n".repeat(20_000);
        assertThat(huge.length()).isGreaterThan(GherkinValidator.MAX_SOURCE_LENGTH);

        assertThatThrownBy(() -> validator.validate(huge))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex ->
                        assertThat(((DomainValidationException) ex).getDetail()).containsEntry("field", "source"));
    }

    @Test
    void rejectsFeatureWithNoScenarios() {
        var src =
                """
                Feature: Empty feature
                  This feature has a description but no scenarios.
                """;

        assertThatThrownBy(() -> validator.validate(src))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex ->
                        assertThat(((DomainValidationException) ex).getDetail()).containsEntry("keyword", "Scenario"));
    }

    @Test
    void rejectsScenarioWithNoSteps() {
        var src = """
                Feature: Sign in

                  Scenario: Step-less
                """;

        assertThatThrownBy(() -> validator.validate(src))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex ->
                        assertThat(((DomainValidationException) ex).getDetail()).containsEntry("keyword", "Steps"));
    }

    @Test
    void rejectsScenarioOutlineWithoutExamples() {
        // The Gherkin grammar treats Scenario / Scenario Outline as one
        // syntactic node; the parser does not require Examples on an
        // outline. The validator enforces the requirement-level expectation
        // that "Scenario Outline" carry at least one Examples block.
        var src =
                """
                Feature: Sign in

                  Scenario Outline: missing examples
                    Given a "<value>" placeholder
                """;

        assertThatThrownBy(() -> validator.validate(src))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex ->
                        assertThat(((DomainValidationException) ex).getDetail()).containsEntry("keyword", "Examples"));
    }

    @Test
    void rejectsExamplesWithoutDataRows() {
        var src =
                """
                Feature: Sign in

                  Scenario Outline: header only
                    Given a "<value>" placeholder
                    Examples:
                      | value |
                """;

        assertThatThrownBy(() -> validator.validate(src))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex ->
                        assertThat(((DomainValidationException) ex).getDetail()).containsEntry("keyword", "Examples"));
    }

    @Test
    void rejectsExamplesTableExceedingRowCap() {
        var rows = new StringBuilder();
        for (int i = 0; i < GherkinValidator.MAX_EXAMPLES_ROWS + 1; i++) {
            rows.append("      | v").append(i).append(" |\n");
        }
        var src =
                """
                Feature: Sign in

                  Scenario Outline: too many rows
                    Given a "<value>" placeholder
                    Examples:
                      | value |
                """
                        + rows;

        assertThatThrownBy(() -> validator.validate(src))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex ->
                        assertThat(((DomainValidationException) ex).getDetail()).containsEntry("field", "examples"));
    }

    @Test
    void rejectsExamplesCellExceedingCellLengthCap() {
        var big = "x".repeat(GherkinValidator.MAX_EXAMPLES_CELL_LENGTH + 1);
        var src =
                """
                Feature: Sign in

                  Scenario Outline: huge cell
                    Given a "<value>" placeholder
                    Examples:
                      | value |
                      | %s |
                """
                        .formatted(big);

        assertThatThrownBy(() -> validator.validate(src)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsFeatureExceedingScenarioCap() {
        var body = new StringBuilder("Feature: many\n\n");
        for (int i = 0; i < GherkinValidator.MAX_SCENARIOS + 1; i++) {
            body.append("  Scenario: case ").append(i).append("\n");
            body.append("    Given step ").append(i).append("\n\n");
        }

        var src = body.toString();
        assertThatThrownBy(() -> validator.validate(src))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex -> assertThat(((DomainValidationException) ex).getDetail())
                        .containsEntry("maxScenarios", String.valueOf(GherkinValidator.MAX_SCENARIOS)));
    }

    @Test
    void validatesScenariosNestedInsideRuleBlocks() {
        // Modern Gherkin allows scenarios under Rule blocks; the validator
        // must apply the same per-scenario contract there too. A Scenario
        // Outline without Examples nested under a Rule must fail validation
        // (matches the codex review finding for Rule-nested scenarios).
        var src =
                """
                Feature: Sign in

                  Rule: every sign-in path

                    Scenario Outline: rule-nested outline missing examples
                      Given a "<value>" placeholder
                """;

        assertThatThrownBy(() -> validator.validate(src))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex ->
                        assertThat(((DomainValidationException) ex).getDetail()).containsEntry("keyword", "Examples"));
    }

    @Test
    void rejectsExamplesHeaderCellExceedingCellLengthCap() {
        // The codex review finding: header cells were previously exempt from
        // the cell-length cap. Authored parameter names are user-controlled
        // content and ADR-042 requires the cap to apply to every cell.
        var big = "x".repeat(GherkinValidator.MAX_EXAMPLES_CELL_LENGTH + 1);
        var src =
                """
                Feature: Sign in

                  Scenario Outline: huge header cell
                    Given a "<%s>" placeholder
                    Examples:
                      | %s |
                      | v |
                """
                        .formatted(big, big);

        assertThatThrownBy(() -> validator.validate(src))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex ->
                        assertThat(((DomainValidationException) ex).getDetail()).containsEntry("field", "examples"));
    }

    @Test
    void errorDetailsNeverLeakSourceText() {
        // Source contains a distinctive token; the parser failure must not echo it.
        var src =
                """
                Feature: leak-check

                  ScenariSEKRETTOKEN: typo'd keyword
                    Given x
                """;

        assertThatThrownBy(() -> validator.validate(src))
                .isInstanceOf(DomainValidationException.class)
                .satisfies(ex -> {
                    var details = ((DomainValidationException) ex).getDetail();
                    for (var entry : details.entrySet()) {
                        assertThat(String.valueOf(entry.getValue()))
                                .as("detail %s leaked source token", entry.getKey())
                                .doesNotContain("SEKRETTOKEN");
                    }
                    assertThat(ex.getMessage()).doesNotContain("SEKRETTOKEN");
                });
    }
}
