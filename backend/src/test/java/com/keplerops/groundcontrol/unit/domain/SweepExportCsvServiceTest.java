package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateEvaluationResult;
import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateResult;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.StatusDriftResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepExportCsvService;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import com.keplerops.groundcontrol.domain.requirements.state.ConfidenceLevel;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import com.keplerops.groundcontrol.domain.requirements.state.StatusDriftSignal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SweepExportCsvServiceTest {

    private final SweepExportCsvService service = new SweepExportCsvService();

    @Test
    void toCsv_emptyReport_containsSummarySection() {
        var report = emptyReport();
        var csv = service.toCsv(report);
        assertThat(csv).contains("# Sweep Report");
        assertThat(csv).contains("test-project");
        assertThat(csv).contains("false"); // hasProblems
    }

    @Test
    void toCsv_reportWithProblems_containsAllSections() {
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
        var completeness = new CompletenessResult(5, Map.of("DRAFT", 3, "ACTIVE", 2), List.of());
        var gate = new QualityGateResult(
                UUID.randomUUID(), "Coverage Gate", "COVERAGE", "IMPLEMENTS", null, "GTE", 80.0, 60.0, false);
        var qgResult = new QualityGateEvaluationResult("test-project", Instant.now(), false, 1, 0, 1, List.of(gate));

        var report = new SweepReport(
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

        var csv = service.toCsv(report);
        assertThat(csv).contains("# Cycles");
        assertThat(csv).contains("GC-001 -> GC-002");
        assertThat(csv).contains("# Orphans");
        assertThat(csv).contains("GC-003");
        assertThat(csv).contains("# Coverage Gaps");
        assertThat(csv).contains("IMPLEMENTS");
        assertThat(csv).contains("# Cross-Wave Violations");
        assertThat(csv).contains("# Consistency Violations");
        assertThat(csv).contains("# Status Drift");
        assertThat(csv).contains("GC-T010");
        assertThat(csv).contains("IMPLEMENTS_LINK_ON_DRAFT");
        assertThat(csv).contains("826"); // evidence artifact identifier
        assertThat(csv).contains("https://github.com/KeplerOps/Ground-Control/issues/826"); // evidence artifact url
        assertThat(csv).contains("# Quality Gates");
        assertThat(csv).contains("Coverage Gate");
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
}
