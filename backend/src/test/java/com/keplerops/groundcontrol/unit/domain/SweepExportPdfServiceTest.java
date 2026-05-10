package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateEvaluationResult;
import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateResult;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessIssue;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.StatusDriftResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepExportPdfService;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import com.keplerops.groundcontrol.domain.requirements.state.ConfidenceLevel;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.StatusDriftSignal;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SweepExportPdfServiceTest {

    private final SweepExportPdfService service = new SweepExportPdfService();

    @Test
    void toPdf_producesValidPdfBytes() {
        var report = emptyReport();
        byte[] bytes = service.toPdf(report);
        assertThat(bytes.length).isGreaterThan(0);
        assertThat(new String(bytes, 0, 5, StandardCharsets.UTF_8)).startsWith("%PDF");
    }

    @Test
    void toPdf_withOrphans_producesLargerOutput() {
        var emptyBytes = service.toPdf(emptyReport());
        var orphan = new SweepReport.RequirementSummary("GC-001", "Orphan");
        var reportWithOrphan = new SweepReport(
                "test-project",
                Instant.now(),
                List.of(),
                List.of(orphan),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                new CompletenessResult(1, Map.of("DRAFT", 1), List.of()),
                null);
        var orphanBytes = service.toPdf(reportWithOrphan);
        assertThat(orphanBytes.length).isGreaterThan(emptyBytes.length);
    }

    @Test
    void toPdf_withFullData_producesValidPdf() {
        var report = fullReport();
        byte[] bytes = service.toPdf(report);
        assertThat(bytes.length).isGreaterThan(0);
        assertThat(new String(bytes, 0, 5, StandardCharsets.UTF_8)).startsWith("%PDF");
        // Full report should be significantly larger than empty report
        assertThat(bytes.length).isGreaterThan(service.toPdf(emptyReport()).length);
    }

    @Test
    void toPdf_rendersStatusDriftSectionWithEvidenceArtifact() throws IOException {
        // Extract the rendered text so a no-op'd addStatusDrift would be caught: GC-T010 and the
        // evidence artifact identifier (826) appear only in the status-drift section of fullReport().
        var text = extractText(service.toPdf(fullReport()));
        assertThat(text).contains("Status Drift");
        assertThat(text).contains("GC-T010");
        assertThat(text).contains("826");
    }

    private static String extractText(byte[] pdfBytes) throws IOException {
        var reader = new PdfReader(pdfBytes);
        try {
            var extractor = new PdfTextExtractor(reader);
            var sb = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extractor.getTextFromPage(page)).append('\n');
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    private SweepReport emptyReport() {
        return new SweepReport(
                "test-project",
                Instant.now(),
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                new CompletenessResult(0, Map.of(), List.of()),
                null);
    }

    private SweepReport fullReport() {
        var cycle = new CycleResult(
                List.of("GC-001", "GC-002"), List.of(new CycleEdge("GC-001", "GC-002", RelationType.DEPENDS_ON)));
        var orphan = new SweepReport.RequirementSummary("GC-003", "Orphan Req");
        var coverageGap = Map.of("IMPLEMENTS", List.of(new SweepReport.RequirementSummary("GC-004", "Uncovered")));
        var crossWave = List.of(new SweepReport.CrossWaveViolationSummary("GC-005", 1, "GC-006", 2, "DEPENDS_ON"));
        var consistency = List.of(
                new SweepReport.ConsistencyViolationSummary("GC-007", "ACTIVE", "GC-008", "ACTIVE", "ACTIVE_CONFLICT"));
        var statusDrift = List.of(new StatusDriftResult.Finding(
                "GC-T010",
                "Risk Assessment Result Entity",
                ConfidenceLevel.HIGH,
                StatusDriftSignal.IMPLEMENTS_LINK_ON_DRAFT,
                List.of(new StatusDriftResult.Evidence(
                        StatusDriftSignal.IMPLEMENTS_LINK_ON_DRAFT,
                        ConfidenceLevel.HIGH,
                        "GITHUB_ISSUE",
                        "826",
                        "GC-T010: Risk Assessment Result Entity",
                        "https://github.com/KeplerOps/Ground-Control/issues/826",
                        "IMPLEMENTS link on a DRAFT requirement"))));
        var completeness = new CompletenessResult(
                5, Map.of("DRAFT", 3, "ACTIVE", 2), List.of(new CompletenessIssue("GC-009", "missing statement")));
        var gate = new QualityGateResult(
                UUID.randomUUID(), "Coverage Gate", "COVERAGE", "IMPLEMENTS", null, "GTE", 80.0, 60.0, false);
        var qgResult = new QualityGateEvaluationResult("test-project", Instant.now(), false, 1, 0, 1, List.of(gate));

        return new SweepReport(
                "test-project",
                Instant.now(),
                List.of(cycle),
                List.of(orphan),
                coverageGap,
                crossWave,
                consistency,
                statusDrift,
                completeness,
                qgResult);
    }
}
