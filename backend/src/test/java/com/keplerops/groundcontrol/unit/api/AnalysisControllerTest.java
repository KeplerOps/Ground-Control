package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.AnalysisController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisService;
import com.keplerops.groundcontrol.domain.requirements.service.CoverageStats;
import com.keplerops.groundcontrol.domain.requirements.service.CycleEdge;
import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import com.keplerops.groundcontrol.domain.requirements.service.DashboardStats;
import com.keplerops.groundcontrol.domain.requirements.service.RecentChange;
import com.keplerops.groundcontrol.domain.requirements.service.WaveStats;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisService analysisService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private com.keplerops.groundcontrol.domain.requirements.service.SimilarityService similarityService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Project TEST_PROJECT = createTestProject();

    private static Project createTestProject() {
        var project = new Project("test-project", "Test Project");
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, PROJECT_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    @BeforeEach
    void setUp() {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
    }

    private static Requirement makeRequirement(String uid, UUID id) {
        var req = new Requirement(TEST_PROJECT, uid, "Title for " + uid, "Statement for " + uid);
        setField(req, "id", id);
        return req;
    }

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class DetectCycles {

        @Test
        void returns200() throws Exception {
            var edges = List.of(
                    new CycleEdge("REQ-A", "REQ-B", RelationType.DEPENDS_ON),
                    new CycleEdge("REQ-B", "REQ-A", RelationType.PARENT));
            var cycleResult = new CycleResult(List.of("REQ-A", "REQ-B", "REQ-A"), edges);
            when(analysisService.detectCycles(PROJECT_ID)).thenReturn(List.of(cycleResult));

            mockMvc.perform(get("/api/v1/analysis/cycles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].members", hasSize(3)))
                    .andExpect(jsonPath("$[0].members[0]", is("REQ-A")))
                    .andExpect(jsonPath("$[0].edges", hasSize(2)))
                    .andExpect(jsonPath("$[0].edges[0].sourceUid", is("REQ-A")))
                    .andExpect(jsonPath("$[0].edges[0].targetUid", is("REQ-B")))
                    .andExpect(jsonPath("$[0].edges[0].relationType", is("DEPENDS_ON")))
                    .andExpect(jsonPath("$[0].edges[1].relationType", is("PARENT")));
        }
    }

    @Nested
    class FindOrphans {

        @Test
        void returns200() throws Exception {
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-ORPHAN", reqId);
            when(analysisService.findOrphans(PROJECT_ID)).thenReturn(List.of(req));

            mockMvc.perform(get("/api/v1/analysis/orphans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].uid", is("REQ-ORPHAN")));
        }
    }

    @Nested
    class FindCoverageGaps {

        @Test
        void returns200() throws Exception {
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-GAP", reqId);
            when(analysisService.findCoverageGaps(PROJECT_ID, LinkType.TESTS)).thenReturn(List.of(req));

            mockMvc.perform(get("/api/v1/analysis/coverage-gaps").param("linkType", "TESTS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].uid", is("REQ-GAP")));
        }

        @Test
        void withMissingLinkType_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/analysis/coverage-gaps")).andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ImpactAnalysis {

        @Test
        void returns200() throws Exception {
            UUID reqId = UUID.randomUUID();
            var req = makeRequirement("REQ-IMPACT", reqId);
            when(analysisService.impactAnalysis(any(UUID.class))).thenReturn(Set.of(req));

            mockMvc.perform(get("/api/v1/analysis/impact/" + reqId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].uid", is("REQ-IMPACT")));
        }
    }

    @Nested
    class CrossWaveValidation {

        @Test
        void returns200() throws Exception {
            UUID aId = UUID.randomUUID();
            UUID bId = UUID.randomUUID();
            var a = makeRequirement("REQ-A", aId);
            a.setWave(1);
            var b = makeRequirement("REQ-B", bId);
            b.setWave(3);
            var rel = new RequirementRelation(a, b, RelationType.DEPENDS_ON);
            setField(rel, "id", UUID.randomUUID());

            when(analysisService.crossWaveValidation(PROJECT_ID)).thenReturn(List.of(rel));

            mockMvc.perform(get("/api/v1/analysis/cross-wave"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].sourceUid", is("REQ-A")))
                    .andExpect(jsonPath("$[0].targetUid", is("REQ-B")));
        }
    }

    @Nested
    class GetDashboardStats {

        @Test
        void returns200WithExpectedStructure() throws Exception {
            Map<String, Integer> byStatus = new LinkedHashMap<>();
            byStatus.put("DRAFT", 2);
            byStatus.put("ACTIVE", 1);

            var waveStats = List.of(
                    new WaveStats(1, 2, Map.of("DRAFT", 1, "ACTIVE", 1)), new WaveStats(2, 1, Map.of("DRAFT", 1)));

            Map<String, CoverageStats> coverage = new LinkedHashMap<>();
            coverage.put("IMPLEMENTS", new CoverageStats(3, 1, 33.3));
            coverage.put("TESTS", new CoverageStats(3, 0, 0.0));

            var recentChanges = List.of(
                    new RecentChange("REQ-A", "Title A", "MOD", Instant.parse("2026-03-18T10:00:00Z"), "user1", null));

            var stats = new DashboardStats(3, byStatus, waveStats, coverage, recentChanges);

            when(analysisService.getDashboardStats(PROJECT_ID)).thenReturn(stats);

            mockMvc.perform(get("/api/v1/analysis/dashboard-stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRequirements", is(3)))
                    .andExpect(jsonPath("$.byStatus.DRAFT", is(2)))
                    .andExpect(jsonPath("$.byStatus.ACTIVE", is(1)))
                    .andExpect(jsonPath("$.byWave", hasSize(2)))
                    .andExpect(jsonPath("$.byWave[0].wave", is(1)))
                    .andExpect(jsonPath("$.byWave[0].total", is(2)))
                    .andExpect(jsonPath("$.coverageByLinkType.IMPLEMENTS.covered", is(1)))
                    .andExpect(jsonPath("$.coverageByLinkType.IMPLEMENTS.percentage", is(33.3)))
                    .andExpect(jsonPath("$.recentChanges", hasSize(1)))
                    .andExpect(jsonPath("$.recentChanges[0].uid", is("REQ-A")))
                    .andExpect(jsonPath("$.recentChanges[0].revisionType", is("MOD")));
        }
    }
}
