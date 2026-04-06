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

import com.keplerops.groundcontrol.api.riskscenarios.RiskRegisterRecordController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskRegisterRecord;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.service.RiskRegisterRecordService;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskRegisterStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RiskRegisterRecordController.class)
class RiskRegisterRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskRegisterRecordService riskRegisterRecordService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final UUID SCENARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000300");
    private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");
    private static final Instant NEXT_REVIEW = Instant.parse("2026-07-01T00:00:00Z");

    private RiskRegisterRecord makeRecord() {
        return makeRecord(true);
    }

    private RiskRegisterRecord makeRecord(boolean withScenarios) {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);

        var record = new RiskRegisterRecord(project, "RRR-001", "Credential Theft Register");
        record.setOwner("Security Lead");
        record.setReviewCadence("QUARTERLY");
        record.setNextReviewAt(NEXT_REVIEW);
        record.setCategoryTags(List.of("security", "identity"));
        record.setDecisionMetadata(Map.of("priority", "high"));
        record.setAssetScopeSummary("IAM subsystem");
        setField(record, "id", RECORD_ID);
        setField(record, "createdAt", NOW);
        setField(record, "updatedAt", NOW);

        if (withScenarios) {
            var scenario =
                    new RiskScenario(project, "RS-001", "Credential theft", "Attacker", "Phishing", "IAM", "Breach");
            scenario.setTimeHorizon("12 months");
            setField(scenario, "id", SCENARIO_ID);
            record.replaceRiskScenarios(List.of(scenario));
        }

        return record;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(riskRegisterRecordService.create(any())).thenReturn(makeRecord());

        mockMvc.perform(post("/api/v1/risk-register-records")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "uid": "RRR-001",
                                  "title": "Credential Theft Register",
                                  "owner": "Security Lead",
                                  "reviewCadence": "QUARTERLY",
                                  "nextReviewAt": "2026-07-01T00:00:00Z",
                                  "categoryTags": ["security", "identity"],
                                  "decisionMetadata": {"priority": "high"},
                                  "assetScopeSummary": "IAM subsystem",
                                  "riskScenarioIds": ["%s"]
                                }
                                """
                                        .formatted(SCENARIO_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(RECORD_ID.toString())))
                .andExpect(jsonPath("$.graphNodeId", is("RISK_REGISTER_RECORD:" + RECORD_ID)))
                .andExpect(jsonPath("$.projectIdentifier", is("ground-control")))
                .andExpect(jsonPath("$.uid", is("RRR-001")))
                .andExpect(jsonPath("$.title", is("Credential Theft Register")))
                .andExpect(jsonPath("$.owner", is("Security Lead")))
                .andExpect(jsonPath("$.status", is("IDENTIFIED")))
                .andExpect(jsonPath("$.reviewCadence", is("QUARTERLY")))
                .andExpect(jsonPath("$.categoryTags", hasSize(2)))
                .andExpect(jsonPath("$.categoryTags[0]", is("security")))
                .andExpect(jsonPath("$.assetScopeSummary", is("IAM subsystem")))
                .andExpect(jsonPath("$.riskScenarioIds", hasSize(1)))
                .andExpect(jsonPath("$.riskScenarioIds[0]", is(SCENARIO_ID.toString())))
                .andExpect(jsonPath("$.riskScenarioUids", hasSize(1)))
                .andExpect(jsonPath("$.riskScenarioUids[0]", is("RS-001")));
    }

    @Test
    void createReturns422WhenUidMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/risk-register-records")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "title": "Credential Theft Register"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createReturns422WhenTitleMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/risk-register-records")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "uid": "RRR-001"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createWithNoScenariosReturnsEmptyLists() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(riskRegisterRecordService.create(any())).thenReturn(makeRecord(false));

        mockMvc.perform(
                        post("/api/v1/risk-register-records")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "uid": "RRR-001",
                                  "title": "Credential Theft Register"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskScenarioIds", hasSize(0)))
                .andExpect(jsonPath("$.riskScenarioUids", hasSize(0)));
    }

    @Test
    void listReturnsRecordsByProject() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(riskRegisterRecordService.listByProject(PROJECT_ID)).thenReturn(List.of(makeRecord()));

        mockMvc.perform(get("/api/v1/risk-register-records").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(RECORD_ID.toString())))
                .andExpect(jsonPath("$[0].uid", is("RRR-001")));
    }

    @Test
    void listReturnsEmptyWhenNone() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(riskRegisterRecordService.listByProject(PROJECT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/risk-register-records").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getByIdReturnsRecord() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(riskRegisterRecordService.getById(PROJECT_ID, RECORD_ID)).thenReturn(makeRecord());

        mockMvc.perform(get("/api/v1/risk-register-records/{id}", RECORD_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(RECORD_ID.toString())))
                .andExpect(jsonPath("$.uid", is("RRR-001")))
                .andExpect(jsonPath("$.title", is("Credential Theft Register")))
                .andExpect(jsonPath("$.owner", is("Security Lead")))
                .andExpect(jsonPath("$.status", is("IDENTIFIED")));
    }

    @Test
    void updateReturnsRecord() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(riskRegisterRecordService.update(eq(PROJECT_ID), eq(RECORD_ID), any()))
                .thenReturn(makeRecord());

        mockMvc.perform(
                        put("/api/v1/risk-register-records/{id}", RECORD_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "title": "Updated Register",
                                  "owner": "CISO",
                                  "reviewCadence": "MONTHLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(RECORD_ID.toString())));
    }

    @Test
    void transitionStatusReturnsRecord() throws Exception {
        var record = makeRecord();
        record.transitionStatus(RiskRegisterStatus.ANALYZING);
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(riskRegisterRecordService.transitionStatus(PROJECT_ID, RECORD_ID, RiskRegisterStatus.ANALYZING))
                .thenReturn(record);

        mockMvc.perform(
                        put("/api/v1/risk-register-records/{id}/status", RECORD_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"status": "ANALYZING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ANALYZING")));
    }

    @Test
    void transitionStatusReturns422WhenStatusMissing() throws Exception {
        mockMvc.perform(put("/api/v1/risk-register-records/{id}/status", RECORD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/risk-register-records/{id}", RECORD_ID).param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(riskRegisterRecordService).delete(PROJECT_ID, RECORD_ID);
    }
}
