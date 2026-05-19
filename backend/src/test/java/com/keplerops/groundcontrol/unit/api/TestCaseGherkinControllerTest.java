package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.containsString;
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

import com.keplerops.groundcontrol.api.testcases.TestCaseGherkinController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseGherkin;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseGherkinService;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(TestCaseGherkinController.class)
class TestCaseGherkinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestCaseGherkinService gherkinService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEST_CASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000700");
    private static final UUID GHERKIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000900");
    private static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");
    private static final String VALID_SOURCE =
            """
            Feature: Sign in

              Scenario: Successful sign-in
                Given the user is on the sign-in page
                When they submit valid credentials
                Then they are redirected to the dashboard
            """;

    private TestCaseGherkin makeGherkin() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var testCase = new TestCase(
                project, "TC-G01", "Sign in", TestCaseType.MANUAL, TestCasePriority.HIGH, TestCaseFormat.GHERKIN);
        setField(testCase, "id", TEST_CASE_ID);
        var gherkin = new TestCaseGherkin(testCase, VALID_SOURCE);
        setField(gherkin, "id", GHERKIN_ID);
        setField(gherkin, "createdAt", NOW);
        setField(gherkin, "updatedAt", NOW);
        return gherkin;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(gherkinService.create(any())).thenReturn(makeGherkin());

        mockMvc.perform(post("/api/v1/test-cases/{testCaseId}/gherkin", TEST_CASE_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":" + jsonString(VALID_SOURCE) + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(GHERKIN_ID.toString())))
                .andExpect(jsonPath("$.testCaseId", is(TEST_CASE_ID.toString())))
                .andExpect(jsonPath("$.source", containsString("Scenario: Successful sign-in")));
    }

    @Test
    void createRejectsBlankSource() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        // Pin not just the status but the envelope contract:
        // MethodArgumentNotValidException → code=validation_error with detail
        // keyed by the offending field name. Without these the test passes
        // for any 422 cause, including unrelated bean-validation failures
        // elsewhere in the request shape.
        mockMvc.perform(post("/api/v1/test-cases/{testCaseId}/gherkin", TEST_CASE_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("validation_error")))
                .andExpect(jsonPath("$.error.detail.source").exists());
    }

    @Test
    void getReturnsGherkin() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(gherkinService.getByTestCase(PROJECT_ID, TEST_CASE_ID)).thenReturn(makeGherkin());

        mockMvc.perform(get("/api/v1/test-cases/{testCaseId}/gherkin", TEST_CASE_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(GHERKIN_ID.toString())))
                .andExpect(jsonPath("$.source", containsString("Feature: Sign in")));
    }

    @Test
    void updateReturnsUpdatedGherkin() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(gherkinService.update(any(), any(), any())).thenReturn(makeGherkin());

        mockMvc.perform(put("/api/v1/test-cases/{testCaseId}/gherkin", TEST_CASE_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":" + jsonString(VALID_SOURCE) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source", containsString("Feature: Sign in")));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/test-cases/{testCaseId}/gherkin", TEST_CASE_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(gherkinService).deleteByTestCase(PROJECT_ID, TEST_CASE_ID);
    }

    private static String jsonString(String raw) {
        return "\""
                + raw.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                + "\"";
    }
}
