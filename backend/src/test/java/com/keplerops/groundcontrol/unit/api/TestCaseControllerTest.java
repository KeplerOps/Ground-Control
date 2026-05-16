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

import com.keplerops.groundcontrol.api.testcases.TestCaseController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseService;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(TestCaseController.class)
class TestCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestCaseService testCaseService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEST_CASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000700");
    private static final Instant NOW = Instant.parse("2026-05-16T05:00:00Z");

    private TestCase makeTestCase() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var testCase = new TestCase(project, "TC-001", "Login flow", TestCaseType.MANUAL, TestCasePriority.HIGH);
        testCase.setDescription("# overview");
        testCase.setPreconditions("- logged in");
        testCase.setPostconditions("- session cleared");
        testCase.setEstimatedDurationSeconds(300L);
        setField(testCase, "id", TEST_CASE_ID);
        setField(testCase, "createdAt", NOW);
        setField(testCase, "updatedAt", NOW);
        return testCase;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testCaseService.create(any())).thenReturn(makeTestCase());

        mockMvc.perform(
                        post("/api/v1/test-cases")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "uid": "TC-001",
                                  "title": "Login flow",
                                  "type": "MANUAL",
                                  "priority": "HIGH",
                                  "estimatedDurationSeconds": 300
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(TEST_CASE_ID.toString())))
                .andExpect(jsonPath("$.uid", is("TC-001")))
                .andExpect(jsonPath("$.title", is("Login flow")))
                .andExpect(jsonPath("$.type", is("MANUAL")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.graphNodeId").doesNotExist());
    }

    @Test
    void createRejectsMissingRequiredFields() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        post("/api/v1/test-cases")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"uid": "TC-001", "title": "x"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createRejectsBlankUid() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        post("/api/v1/test-cases")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"uid": "", "title": "x", "type": "MANUAL", "priority": "LOW"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createRejectsNegativeDuration() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        post("/api/v1/test-cases")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"uid": "TC-001", "title": "x", "type": "MANUAL",
                                 "priority": "LOW", "estimatedDurationSeconds": -10}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listReturnsTestCases() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testCaseService.listByProject(PROJECT_ID)).thenReturn(List.of(makeTestCase()));

        mockMvc.perform(get("/api/v1/test-cases").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("TC-001")));
    }

    @Test
    void getByIdReturnsTestCase() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testCaseService.getById(PROJECT_ID, TEST_CASE_ID)).thenReturn(makeTestCase());

        mockMvc.perform(get("/api/v1/test-cases/{id}", TEST_CASE_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("TC-001")));
    }

    @Test
    void getByUidReturnsTestCase() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testCaseService.getByUid("TC-001", PROJECT_ID)).thenReturn(makeTestCase());

        mockMvc.perform(get("/api/v1/test-cases/uid/{uid}", "TC-001").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("TC-001")));
    }

    @Test
    void updateReturnsUpdatedTestCase() throws Exception {
        var testCase = makeTestCase();
        testCase.setTitle("Updated Title");
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testCaseService.update(eq(PROJECT_ID), eq(TEST_CASE_ID), any())).thenReturn(testCase);

        mockMvc.perform(
                        put("/api/v1/test-cases/{id}", TEST_CASE_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"title":"Updated Title"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/test-cases/{id}", TEST_CASE_ID).param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(testCaseService).delete(PROJECT_ID, TEST_CASE_ID);
    }

    @Test
    void transitionStatusReturnsTestCase() throws Exception {
        var testCase = makeTestCase();
        testCase.transitionStatus(TestCaseStatus.APPROVED);
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testCaseService.transitionStatus(PROJECT_ID, TEST_CASE_ID, TestCaseStatus.APPROVED))
                .thenReturn(testCase);

        mockMvc.perform(
                        put("/api/v1/test-cases/{id}/status", TEST_CASE_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"status":"APPROVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    void transitionStatusRejectsUnknownEnumValue() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        put("/api/v1/test-cases/{id}/status", TEST_CASE_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"status":"NOT_A_STATUS"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
