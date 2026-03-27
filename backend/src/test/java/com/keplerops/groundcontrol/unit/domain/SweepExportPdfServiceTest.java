package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateEvaluationResult;
import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateResult;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessIssue;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepExportPdfService;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
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

    private SweepReport emptyReport() {
        return new SweepReport(
                "test-project",
                Instant.now(),
                List.of(),
                List.of(),
                Map.of(),
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
                completeness,
                qgResult);
    }
}
