package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.qualitygates.QualityGateController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.qualitygates.model.QualityGate;
import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateEvaluationResult;
import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateResult;
import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateService;
import com.keplerops.groundcontrol.domain.qualitygates.state.ComparisonOperator;
import com.keplerops.groundcontrol.domain.qualitygates.state.MetricType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QualityGateController.class)
class QualityGateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QualityGateService qualityGateService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID GATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("test")).thenReturn(PROJECT_ID);
        when(qualityGateService.create(any())).thenReturn(makeGate());

        mockMvc.perform(
                        post("/api/v1/quality-gates")
                                .param("project", "test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"name":"Coverage Gate","metricType":"COVERAGE","metricParam":"TESTS",
                 "scopeStatus":"ACTIVE","operator":"GTE","threshold":80.0}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Coverage Gate")))
                .andExpect(jsonPath("$.metricType", is("COVERAGE")));
    }

    @Test
    void listReturnsGates() throws Exception {
        when(projectService.resolveProjectId("test")).thenReturn(PROJECT_ID);
        when(qualityGateService.listByProject(PROJECT_ID)).thenReturn(List.of(makeGate()));

        mockMvc.perform(get("/api/v1/quality-gates").param("project", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Coverage Gate")));
    }

    @Test
    void getByIdReturnsGate() throws Exception {
        when(qualityGateService.getById(GATE_ID)).thenReturn(makeGate());

        mockMvc.perform(get("/api/v1/quality-gates/{id}", GATE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Coverage Gate")));
    }

    @Test
    void updateReturnsUpdatedGate() throws Exception {
        when(qualityGateService.update(any(), any())).thenReturn(makeGate());

        mockMvc.perform(put("/api/v1/quality-gates/{id}", GATE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"name":"Coverage Gate","threshold":90.0}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Coverage Gate")));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/quality-gates/{id}", GATE_ID)).andExpect(status().isNoContent());

        verify(qualityGateService).delete(GATE_ID);
    }

    @Test
    void evaluateReturnsResults() throws Exception {
        var gateResult = new QualityGateResult(
                GATE_ID, "Coverage Gate", "COVERAGE", "TESTS", "ACTIVE", "GTE", 80.0, 65.0, false);
        var evalResult = new QualityGateEvaluationResult(
                "test", Instant.parse("2026-03-24T06:00:00Z"), false, 1, 0, 1, List.of(gateResult));
        when(qualityGateService.evaluate("test")).thenReturn(evalResult);

        mockMvc.perform(post("/api/v1/quality-gates/evaluate").param("project", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed", is(false)))
                .andExpect(jsonPath("$.totalGates", is(1)))
                .andExpect(jsonPath("$.failedCount", is(1)))
                .andExpect(jsonPath("$.gates", hasSize(1)))
                .andExpect(jsonPath("$.gates[0].gateName", is("Coverage Gate")))
                .andExpect(jsonPath("$.gates[0].actualValue", is(65.0)));
    }

    private static QualityGate makeGate() {
        var project = new Project("test", "Test Project");
        setField(project, "id", PROJECT_ID);
        var gate = new QualityGate(
                project,
                "Coverage Gate",
                "Desc",
                MetricType.COVERAGE,
                "TESTS",
                Status.ACTIVE,
                ComparisonOperator.GTE,
                80.0);
        setField(gate, "id", GATE_ID);
        setField(gate, "createdAt", Instant.parse("2026-03-24T06:00:00Z"));
        setField(gate, "updatedAt", Instant.parse("2026-03-24T06:00:00Z"));
        return gate;
    }

    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
