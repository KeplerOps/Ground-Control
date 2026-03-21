package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.SweepController;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisSweepService;
import com.keplerops.groundcontrol.domain.requirements.service.CompletenessResult;
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.SweepReport;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SweepController.class)
class SweepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisSweepService analysisSweepService;

    private static SweepReport cleanReport(String projectIdentifier) {
        return new SweepReport(
                projectIdentifier,
                Instant.parse("2026-03-20T06:00:00Z"),
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                new CompletenessResult(5, Map.of("DRAFT", 3, "ACTIVE", 2), List.of()));
    }

    private static SweepReport reportWithOrphans(String projectIdentifier) {
        return new SweepReport(
                projectIdentifier,
                Instant.parse("2026-03-20T06:00:00Z"),
                List.of(),
                List.of(
                        new SweepReport.RequirementSummary("GC-ORPH1", "Orphan One"),
                        new SweepReport.RequirementSummary("GC-ORPH2", "Orphan Two")),
                Map.of(),
                List.of(),
                List.of(),
                new CompletenessResult(5, Map.of("DRAFT", 3, "ACTIVE", 2), List.of()));
    }

    @Test
    void sweepReturnsReportForProject() throws Exception {
        when(analysisSweepService.sweep("test-project")).thenReturn(cleanReport("test-project"));

        mockMvc.perform(post("/api/v1/analysis/sweep").param("project", "test-project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectIdentifier", is("test-project")))
                .andExpect(jsonPath("$.hasProblems", is(false)))
                .andExpect(jsonPath("$.totalProblems", is(0)));
    }

    @Test
    void sweepReturnsReportWithProblems() throws Exception {
        when(analysisSweepService.sweep(any())).thenReturn(reportWithOrphans("test-project"));

        mockMvc.perform(post("/api/v1/analysis/sweep").param("project", "test-project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProblems", is(true)))
                .andExpect(jsonPath("$.totalProblems", is(2)))
                .andExpect(jsonPath("$.orphans", hasSize(2)))
                .andExpect(jsonPath("$.orphans[0].uid", is("GC-ORPH1")));
    }

    @Test
    void sweepReturnsAllProblemTypes() throws Exception {
        var fullReport = new SweepReport(
                "test-project",
                Instant.parse("2026-03-20T06:00:00Z"),
                List.of(new CycleResult(
                        List.of("GC-A", "GC-B"), List.of(new CycleEdge("GC-A", "GC-B", RelationType.DEPENDS_ON)))),
                List.of(new SweepReport.RequirementSummary("GC-ORPH1", "Orphan")),
                Map.of("IMPLEMENTS", List.of(new SweepReport.RequirementSummary("GC-GAP1", "Gap"))),
                List.of(new SweepReport.CrossWaveViolationSummary("GC-A", 1, "GC-B", 2, "DEPENDS_ON")),
                List.of(new SweepReport.ConsistencyViolationSummary(
                        "GC-X", "ACTIVE", "GC-Y", "ACTIVE", "ACTIVE_CONFLICT")),
                new CompletenessResult(5, Map.of("DRAFT", 3), List.of()));

        when(analysisSweepService.sweep(any())).thenReturn(fullReport);

        mockMvc.perform(post("/api/v1/analysis/sweep").param("project", "test-project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProblems", is(true)))
                .andExpect(jsonPath("$.cycles", hasSize(1)))
                .andExpect(jsonPath("$.cycles[0].members[0]", is("GC-A")))
                .andExpect(jsonPath("$.orphans", hasSize(1)))
                .andExpect(jsonPath("$.coverageGaps.IMPLEMENTS", hasSize(1)))
                .andExpect(jsonPath("$.coverageGaps.IMPLEMENTS[0].uid", is("GC-GAP1")))
                .andExpect(jsonPath("$.crossWaveViolations", hasSize(1)))
                .andExpect(jsonPath("$.crossWaveViolations[0].sourceUid", is("GC-A")))
                .andExpect(jsonPath("$.crossWaveViolations[0].sourceWave", is(1)))
                .andExpect(jsonPath("$.consistencyViolations", hasSize(1)))
                .andExpect(jsonPath("$.consistencyViolations[0].violationType", is("ACTIVE_CONFLICT")));
    }

    @Test
    void sweepAllReturnsReportsForAllProjects() throws Exception {
        when(analysisSweepService.sweepAll())
                .thenReturn(List.of(cleanReport("project-a"), reportWithOrphans("project-b")));

        mockMvc.perform(post("/api/v1/analysis/sweep/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].projectIdentifier", is("project-a")))
                .andExpect(jsonPath("$[0].hasProblems", is(false)))
                .andExpect(jsonPath("$[1].projectIdentifier", is("project-b")))
                .andExpect(jsonPath("$[1].hasProblems", is(true)));
    }
}
