package com.keplerops.groundcontrol.infrastructure.sweep;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GitHubIssueSweepNotifierTest {

    @Test
    void formatsBodyWithAllSections() {
        var report = new SweepReport(
                "test-project",
                Instant.parse("2026-03-20T06:00:00Z"),
                List.of(new CycleResult(
                        List.of("GC-A", "GC-B"), List.of(new CycleEdge("GC-A", "GC-B", RelationType.DEPENDS_ON)))),
                List.of(new SweepReport.RequirementSummary("GC-ORPH1", "Orphan One")),
                Map.of("IMPLEMENTS", List.of(new SweepReport.RequirementSummary("GC-GAP1", "Gap One"))),
                List.of(new SweepReport.CrossWaveViolationSummary("GC-A", 1, "GC-B", 2, "DEPENDS_ON")),
                List.of(new SweepReport.ConsistencyViolationSummary(
                        "GC-X", "ACTIVE", "GC-Y", "ACTIVE", "ACTIVE_CONFLICT")),
                new CompletenessResult(5, Map.of("DRAFT", 3), List.of()));

        var body = GitHubIssueSweepNotifier.formatBody(report);

        assertThat(body).contains("## Analysis Sweep Report");
        assertThat(body).contains("**Project:** test-project");
        assertThat(body).contains("**Total problems:** 5");
        assertThat(body).contains("### Dependency Cycles");
        assertThat(body).contains("GC-A -> GC-B");
        assertThat(body).contains("### Orphan Requirements");
        assertThat(body).contains("GC-ORPH1: Orphan One");
        assertThat(body).contains("### Coverage Gaps");
        assertThat(body).contains("**IMPLEMENTS:**");
        assertThat(body).contains("GC-GAP1: Gap One");
        assertThat(body).contains("### Cross-Wave Violations");
        assertThat(body).contains("GC-A (wave 1) -> GC-B (wave 2)");
        assertThat(body).contains("### Consistency Violations");
        assertThat(body).contains("GC-X [ACTIVE] <-> GC-Y [ACTIVE]: ACTIVE_CONFLICT");
    }

    @Test
    void omitsEmptySections() {
        var report = new SweepReport(
                "test-project",
                Instant.parse("2026-03-20T06:00:00Z"),
                List.of(),
                List.of(new SweepReport.RequirementSummary("GC-ORPH1", "Orphan One")),
                Map.of(),
                List.of(),
                List.of(),
                new CompletenessResult(1, Map.of("DRAFT", 1), List.of()));

        var body = GitHubIssueSweepNotifier.formatBody(report);

        assertThat(body).contains("### Orphan Requirements");
        assertThat(body).doesNotContain("### Dependency Cycles");
        assertThat(body).doesNotContain("### Coverage Gaps");
        assertThat(body).doesNotContain("### Cross-Wave Violations");
        assertThat(body).doesNotContain("### Consistency Violations");
    }
}
