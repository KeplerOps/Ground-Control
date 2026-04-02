package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.riskscenarios.RiskScenarioController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.service.RiskScenarioService;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RiskScenarioController.class)
class RiskScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskScenarioService riskScenarioService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RS_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
    private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");

    private RiskScenario makeScenario() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var rs = new RiskScenario(
                project,
                "RS-001",
                "Credential stuffing on customer portal",
                "External threat actor",
                "Credential stuffing attack",
                "Customer authentication portal",
                "Data breach and unauthorized access",
                "12 months",
                "system");
        rs.setVulnerability("Weak password policy");
        rs.setObservationRefs("OBS-001");
        rs.setTopologyContext("DMZ web tier");
        setField(rs, "id", RS_ID);
        setField(rs, "createdAt", NOW);
        setField(rs, "updatedAt", NOW);
        return rs;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(riskScenarioService.create(any())).thenReturn(makeScenario());

        mockMvc.perform(
                        post("/api/v1/risk-scenarios")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {
                            "uid": "RS-001",
                            "title": "Credential stuffing on customer portal",
                            "threatSource": "External threat actor",
                            "threatEvent": "Credential stuffing attack",
                            "affectedObject": "Customer authentication portal",
                            "vulnerability": "Weak password policy",
                            "consequence": "Data breach and unauthorized access",
                            "timeHorizon": "12 months",
                            "observationRefs": "OBS-001",
                            "topologyContext": "DMZ web tier"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(RS_ID.toString())))
                .andExpect(jsonPath("$.uid", is("RS-001")))
                .andExpect(jsonPath("$.threatSource", is("External threat actor")))
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    void createReturns422WhenUidMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/risk-scenarios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {
                            "title": "Title",
                            "threatSource": "Source",
                            "threatEvent": "Event",
                            "affectedObject": "Object",
                            "consequence": "Consequence",
                            "timeHorizon": "12 months"
                        }
                        """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listReturnsScenarios() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(riskScenarioService.listByProject(PROJECT_ID)).thenReturn(List.of(makeScenario()));

        mockMvc.perform(get("/api/v1/risk-scenarios").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("RS-001")));
    }

    @Test
    void getByIdReturnsScenario() throws Exception {
        when(riskScenarioService.getById(RS_ID)).thenReturn(makeScenario());

        mockMvc.perform(get("/api/v1/risk-scenarios/{id}", RS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(RS_ID.toString())))
                .andExpect(jsonPath("$.projectIdentifier", is("ground-control")));
    }

    @Test
    void getByUidReturnsScenario() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(riskScenarioService.getByUid("RS-001", PROJECT_ID)).thenReturn(makeScenario());

        mockMvc.perform(get("/api/v1/risk-scenarios/uid/RS-001").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("RS-001")));
    }

    @Test
    void updateReturnsUpdatedScenario() throws Exception {
        var updated = makeScenario();
        updated.setTitle("Updated title");
        when(riskScenarioService.update(eq(RS_ID), any())).thenReturn(updated);

        mockMvc.perform(
                        put("/api/v1/risk-scenarios/{id}", RS_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                        {
                            "title": "Updated title"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated title")));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/risk-scenarios/{id}", RS_ID)).andExpect(status().isNoContent());

        verify(riskScenarioService).delete(RS_ID);
    }

    @Test
    void transitionStatusReturnsUpdatedScenario() throws Exception {
        var rs = makeScenario();
        setField(rs, "status", RiskScenarioStatus.IDENTIFIED);
        when(riskScenarioService.transitionStatus(RS_ID, RiskScenarioStatus.IDENTIFIED))
                .thenReturn(rs);

        mockMvc.perform(put("/api/v1/risk-scenarios/{id}/status", RS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"status": "IDENTIFIED"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IDENTIFIED")));
    }
}
