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

import com.keplerops.groundcontrol.api.findings.FindingController;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.findings.model.Finding;
import com.keplerops.groundcontrol.domain.findings.service.CreateFindingCommand;
import com.keplerops.groundcontrol.domain.findings.service.FindingService;
import com.keplerops.groundcontrol.domain.findings.service.UpdateFindingCommand;
import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.time.LocalDate;
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
@WebMvcTest(FindingController.class)
class FindingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindingService findingService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FINDING_ID = UUID.fromString("00000000-0000-0000-0000-000000000200");
    private static final Instant NOW = Instant.parse("2026-05-13T12:00:00Z");
    private static final LocalDate DUE = LocalDate.of(2026, 6, 30);

    private Finding makeFinding() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var f = new Finding(
                project,
                "FIND-001",
                "MFA missing on admin portal",
                FindingType.CONTROL_DEFICIENCY,
                FindingSeverity.HIGH,
                "Admin portal accepts password-only auth.");
        f.setRootCauseAnalysis("Identity provider misconfigured during migration.");
        f.setOwner("alice");
        f.setDueDate(DUE);
        f.setCreatedBy("analyst");
        setField(f, "id", FINDING_ID);
        setField(f, "createdAt", NOW);
        setField(f, "updatedAt", NOW);
        return f;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(findingService.create(any())).thenReturn(makeFinding());

        mockMvc.perform(
                        post("/api/v1/findings")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "uid": "FIND-001",
                                    "title": "MFA missing on admin portal",
                                    "findingType": "CONTROL_DEFICIENCY",
                                    "severity": "HIGH",
                                    "description": "Admin portal accepts password-only auth.",
                                    "rootCauseAnalysis": "Identity provider misconfigured.",
                                    "owner": "alice",
                                    "dueDate": "2026-06-30"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(FINDING_ID.toString())))
                .andExpect(jsonPath("$.graphNodeId", is("FINDING:" + FINDING_ID)))
                .andExpect(jsonPath("$.uid", is("FIND-001")))
                .andExpect(jsonPath("$.findingType", is("CONTROL_DEFICIENCY")))
                .andExpect(jsonPath("$.severity", is("HIGH")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.description", is("Admin portal accepts password-only auth.")))
                .andExpect(jsonPath("$.owner", is("alice")))
                .andExpect(jsonPath("$.dueDate", is("2026-06-30")));

        // Lock in the request→command mapping: the mocked service returns a canned
        // fixture regardless of input, so without a capture the test would still
        // pass if the controller silently dropped or swapped fields.
        var captor = ArgumentCaptor.forClass(CreateFindingCommand.class);
        verify(findingService).create(captor.capture());
        var command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(PROJECT_ID, command.projectId());
        org.junit.jupiter.api.Assertions.assertEquals("FIND-001", command.uid());
        org.junit.jupiter.api.Assertions.assertEquals("MFA missing on admin portal", command.title());
        org.junit.jupiter.api.Assertions.assertEquals(FindingType.CONTROL_DEFICIENCY, command.findingType());
        org.junit.jupiter.api.Assertions.assertEquals(FindingSeverity.HIGH, command.severity());
        org.junit.jupiter.api.Assertions.assertEquals(
                "Admin portal accepts password-only auth.", command.description());
        org.junit.jupiter.api.Assertions.assertEquals("Identity provider misconfigured.", command.rootCauseAnalysis());
        org.junit.jupiter.api.Assertions.assertEquals("alice", command.owner());
        org.junit.jupiter.api.Assertions.assertEquals(DUE, command.dueDate());
    }

    @Test
    void createReturns422WhenRequiredFieldsMissing() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        post("/api/v1/findings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "uid": "",
                                    "title": "",
                                    "description": ""
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createReturns422OnUnknownEnum() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        post("/api/v1/findings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "uid": "FIND-001",
                                    "title": "x",
                                    "findingType": "NOT_A_TYPE",
                                    "severity": "HIGH",
                                    "description": "x"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createReturns409OnConflict() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(findingService.create(any())).thenThrow(new ConflictException("Duplicate UID"));

        mockMvc.perform(
                        post("/api/v1/findings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                    "uid": "FIND-001",
                                    "title": "Dup",
                                    "findingType": "AUDIT_FINDING",
                                    "severity": "LOW",
                                    "description": "x"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void listReturnsAll() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(findingService.listByProject(PROJECT_ID)).thenReturn(List.of(makeFinding()));

        mockMvc.perform(get("/api/v1/findings").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("FIND-001")));
    }

    @Test
    void getByIdReturnsFinding() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(findingService.getById(PROJECT_ID, FINDING_ID)).thenReturn(makeFinding());

        mockMvc.perform(get("/api/v1/findings/{id}", FINDING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("FIND-001")));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(findingService.getById(eq(PROJECT_ID), any())).thenThrow(new NotFoundException("Finding not found"));

        mockMvc.perform(get("/api/v1/findings/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void getByUidReturnsFinding() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(findingService.getByUid("FIND-001", PROJECT_ID)).thenReturn(makeFinding());

        mockMvc.perform(get("/api/v1/findings/uid/{uid}", "FIND-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(FINDING_ID.toString())));
    }

    @Test
    void updateAppliesChanges() throws Exception {
        var f = makeFinding();
        f.setTitle("Updated title");
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(findingService.update(eq(PROJECT_ID), eq(FINDING_ID), any())).thenReturn(f);

        mockMvc.perform(
                        put("/api/v1/findings/{id}", FINDING_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"title": "Updated title", "clearOwner": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated title")));

        var captor = ArgumentCaptor.forClass(UpdateFindingCommand.class);
        verify(findingService).update(eq(PROJECT_ID), eq(FINDING_ID), captor.capture());
        var command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("Updated title", command.title());
        org.junit.jupiter.api.Assertions.assertTrue(command.clearOwner());
        org.junit.jupiter.api.Assertions.assertFalse(command.clearDueDate());
        org.junit.jupiter.api.Assertions.assertFalse(command.clearRootCauseAnalysis());
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/findings/{id}", FINDING_ID)).andExpect(status().isNoContent());
        verify(findingService).delete(PROJECT_ID, FINDING_ID);
    }

    @Test
    void transitionStatusReturnsUpdatedFinding() throws Exception {
        var f = makeFinding();
        setField(f, "status", FindingStatus.REMEDIATION_IN_PROGRESS);
        when(projectService.resolveProjectId(any())).thenReturn(PROJECT_ID);
        when(findingService.transitionStatus(PROJECT_ID, FINDING_ID, FindingStatus.REMEDIATION_IN_PROGRESS))
                .thenReturn(f);

        mockMvc.perform(
                        put("/api/v1/findings/{id}/status", FINDING_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"status": "REMEDIATION_IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REMEDIATION_IN_PROGRESS")));
    }
}
