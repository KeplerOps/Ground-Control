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

import com.keplerops.groundcontrol.api.audits.AuditController;
import com.keplerops.groundcontrol.domain.audits.model.Audit;
import com.keplerops.groundcontrol.domain.audits.service.AuditService;
import com.keplerops.groundcontrol.domain.audits.service.CreateAuditCommand;
import com.keplerops.groundcontrol.domain.audits.service.UpdateAuditCommand;
import com.keplerops.groundcontrol.domain.audits.state.AuditStatus;
import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuditController.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID AUDIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final Instant NOW = Instant.parse("2026-05-18T12:00:00Z");

    private Audit makeAudit() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var a = new Audit(
                project, "AUDIT-001", "Annual compliance audit", AuditType.INTERNAL, "All production systems.");
        a.setObjectives(List.of("Assess controls", "Review policies"));
        a.setTeamMembers(List.of("alice", "bob"));
        a.setCreatedBy("analyst");
        setField(a, "id", AUDIT_ID);
        setField(a, "createdAt", NOW);
        setField(a, "updatedAt", NOW);
        return a;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(auditService.create(any())).thenReturn(makeAudit());

        mockMvc.perform(
                        post("/api/v1/audits")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "uid": "AUDIT-001",
                                    "title": "Annual compliance audit",
                                    "auditType": "INTERNAL",
                                    "scopeDescription": "All production systems.",
                                    "objectives": ["Assess controls", "Review policies"],
                                    "teamMembers": ["alice", "bob"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(AUDIT_ID.toString())))
                .andExpect(jsonPath("$.graphNodeId", is("AUDIT:" + AUDIT_ID)))
                .andExpect(jsonPath("$.uid", is("AUDIT-001")))
                .andExpect(jsonPath("$.auditType", is("INTERNAL")))
                .andExpect(jsonPath("$.status", is("PLANNED")))
                .andExpect(jsonPath("$.scopeDescription", is("All production systems.")))
                .andExpect(jsonPath("$.objectives", hasSize(2)))
                .andExpect(jsonPath("$.teamMembers", hasSize(2)));

        var captor = ArgumentCaptor.forClass(CreateAuditCommand.class);
        verify(auditService).create(captor.capture());
        var command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(PROJECT_ID, command.projectId());
        org.junit.jupiter.api.Assertions.assertEquals("AUDIT-001", command.uid());
        org.junit.jupiter.api.Assertions.assertEquals("Annual compliance audit", command.title());
        org.junit.jupiter.api.Assertions.assertEquals(AuditType.INTERNAL, command.auditType());
        org.junit.jupiter.api.Assertions.assertEquals("All production systems.", command.scopeDescription());
    }

    @Test
    void createReturns422WhenRequiredFieldsMissing() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        post("/api/v1/audits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "uid": "",
                                    "title": "",
                                    "scopeDescription": ""
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createReturns422OnUnknownEnum() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        post("/api/v1/audits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "uid": "AUDIT-001",
                                    "title": "x",
                                    "auditType": "NOT_A_TYPE",
                                    "scopeDescription": "x"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createReturns409OnConflict() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(auditService.create(any())).thenThrow(new ConflictException("Duplicate UID"));

        mockMvc.perform(
                        post("/api/v1/audits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "uid": "AUDIT-001",
                                    "title": "Dup",
                                    "auditType": "INTERNAL",
                                    "scopeDescription": "x"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void listReturnsAll() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(auditService.listByProject(PROJECT_ID)).thenReturn(List.of(makeAudit()));

        mockMvc.perform(get("/api/v1/audits").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("AUDIT-001")));
    }

    @Test
    void getByIdReturnsAudit() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(auditService.getById(PROJECT_ID, AUDIT_ID)).thenReturn(makeAudit());

        mockMvc.perform(get("/api/v1/audits/{id}", AUDIT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("AUDIT-001")));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(auditService.getById(eq(PROJECT_ID), any())).thenThrow(new NotFoundException("Audit not found"));

        mockMvc.perform(get("/api/v1/audits/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void getByUidReturnsAudit() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(auditService.getByUid("AUDIT-001", PROJECT_ID)).thenReturn(makeAudit());

        mockMvc.perform(get("/api/v1/audits/uid/{uid}", "AUDIT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(AUDIT_ID.toString())));
    }

    @Test
    void updateAppliesChanges() throws Exception {
        var a = makeAudit();
        a.setTitle("Updated title");
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(auditService.update(eq(PROJECT_ID), eq(AUDIT_ID), any())).thenReturn(a);

        mockMvc.perform(
                        put("/api/v1/audits/{id}", AUDIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"title": "Updated title", "clearObjectives": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated title")));

        var captor = ArgumentCaptor.forClass(UpdateAuditCommand.class);
        verify(auditService).update(eq(PROJECT_ID), eq(AUDIT_ID), captor.capture());
        var command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("Updated title", command.title());
        org.junit.jupiter.api.Assertions.assertTrue(command.clearObjectives());
        org.junit.jupiter.api.Assertions.assertFalse(command.clearPhases());
        org.junit.jupiter.api.Assertions.assertFalse(command.clearTeamMembers());
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/audits/{id}", AUDIT_ID)).andExpect(status().isNoContent());
        verify(auditService).delete(PROJECT_ID, AUDIT_ID);
    }

    @Test
    void transitionStatusReturnsUpdatedAudit() throws Exception {
        var a = makeAudit();
        setField(a, "status", AuditStatus.IN_PROGRESS);
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(auditService.transitionStatus(PROJECT_ID, AUDIT_ID, AuditStatus.IN_PROGRESS))
                .thenReturn(a);

        mockMvc.perform(
                        put("/api/v1/audits/{id}/status", AUDIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"status": "IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    void transitionStatusReturns422WhenStatusMissing() throws Exception {
        mockMvc.perform(put("/api/v1/audits/{id}/status", AUDIT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
