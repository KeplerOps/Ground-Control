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

import com.keplerops.groundcontrol.api.threatmodels.ThreatModelController;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModel;
import com.keplerops.groundcontrol.domain.threatmodels.service.ThreatModelService;
import com.keplerops.groundcontrol.domain.threatmodels.state.StrideCategory;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ThreatModelController.class)
class ThreatModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ThreatModelService threatModelService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TM_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final Instant NOW = Instant.parse("2026-04-11T12:00:00Z");

    private ThreatModel makeThreatModel() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var tm = new ThreatModel(
                project,
                "TM-001",
                "Credential stuffing on login portal",
                "External threat actor using leaked credentials",
                "Automated credential replay against /login",
                "Account takeover and customer data exposure");
        tm.setStride(StrideCategory.SPOOFING);
        tm.setNarrative("Observed 3x surge after credential dump release.");
        tm.setCreatedBy("analyst");
        setField(tm, "id", TM_ID);
        setField(tm, "createdAt", NOW);
        setField(tm, "updatedAt", NOW);
        return tm;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(threatModelService.create(any())).thenReturn(makeThreatModel());

        mockMvc.perform(
                        post("/api/v1/threat-models")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "uid": "TM-001",
                                    "title": "Credential stuffing on login portal",
                                    "threatSource": "External threat actor using leaked credentials",
                                    "threatEvent": "Automated credential replay against /login",
                                    "effect": "Account takeover and customer data exposure",
                                    "stride": "SPOOFING",
                                    "narrative": "Observed surge after breach dump."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(TM_ID.toString())))
                .andExpect(jsonPath("$.graphNodeId", is("THREAT_MODEL:" + TM_ID)))
                .andExpect(jsonPath("$.uid", is("TM-001")))
                .andExpect(jsonPath("$.threatSource", is("External threat actor using leaked credentials")))
                .andExpect(jsonPath("$.threatEvent", is("Automated credential replay against /login")))
                .andExpect(jsonPath("$.effect", is("Account takeover and customer data exposure")))
                .andExpect(jsonPath("$.stride", is("SPOOFING")))
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    void createReturns422WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/threat-models")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "title": "Missing uid",
                                    "threatSource": "Source",
                                    "threatEvent": "Event",
                                    "effect": "Effect"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createReturns409OnConflict() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(threatModelService.create(any())).thenThrow(new ConflictException("Duplicate UID"));

        mockMvc.perform(
                        post("/api/v1/threat-models")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "uid": "TM-001",
                                    "title": "Dup",
                                    "threatSource": "s",
                                    "threatEvent": "e",
                                    "effect": "x"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void listReturnsThreatModels() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(threatModelService.listByProject(PROJECT_ID)).thenReturn(List.of(makeThreatModel()));

        mockMvc.perform(get("/api/v1/threat-models").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("TM-001")));
    }

    @Test
    void getByIdReturnsThreatModel() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(threatModelService.getById(PROJECT_ID, TM_ID)).thenReturn(makeThreatModel());

        mockMvc.perform(get("/api/v1/threat-models/{id}", TM_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(TM_ID.toString())))
                .andExpect(jsonPath("$.projectIdentifier", is("ground-control")));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(threatModelService.getById(PROJECT_ID, TM_ID)).thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/threat-models/{id}", TM_ID).param("project", "ground-control"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByUidReturnsThreatModel() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(threatModelService.getByUid("TM-001", PROJECT_ID)).thenReturn(makeThreatModel());

        mockMvc.perform(get("/api/v1/threat-models/uid/TM-001").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("TM-001")));
    }

    @Test
    void updateReturnsUpdatedThreatModel() throws Exception {
        var updated = makeThreatModel();
        updated.setTitle("Updated title");
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(threatModelService.update(eq(PROJECT_ID), eq(TM_ID), any())).thenReturn(updated);

        mockMvc.perform(
                        put("/api/v1/threat-models/{id}", TM_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"title": "Updated title"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated title")));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/threat-models/{id}", TM_ID).param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(threatModelService).delete(PROJECT_ID, TM_ID);
    }

    @Test
    void transitionStatusReturnsUpdatedThreatModel() throws Exception {
        var tm = makeThreatModel();
        setField(tm, "status", ThreatModelStatus.ACTIVE);
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(threatModelService.transitionStatus(PROJECT_ID, TM_ID, ThreatModelStatus.ACTIVE))
                .thenReturn(tm);

        mockMvc.perform(
                        put("/api/v1/threat-models/{id}/status", TM_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }
}
