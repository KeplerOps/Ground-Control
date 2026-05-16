package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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

import com.keplerops.groundcontrol.api.testcases.TestCaseStepController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseStep;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestCaseStepCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseStepService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestCaseStepCommand;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(TestCaseStepController.class)
class TestCaseStepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestCaseStepService stepService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEST_CASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000700");
    private static final UUID STEP_ID = UUID.fromString("00000000-0000-0000-0000-000000000800");
    private static final Instant NOW = Instant.parse("2026-05-16T05:00:00Z");

    private TestCaseStep makeStep(int number, String actual) {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var testCase = new TestCase(project, "TC-001", "Login flow", TestCaseType.MANUAL, TestCasePriority.HIGH);
        setField(testCase, "id", TEST_CASE_ID);
        var step = new TestCaseStep(testCase, number, "Open login page", "Page renders");
        step.setActualResult(actual);
        setField(step, "id", STEP_ID);
        setField(step, "createdAt", NOW);
        setField(step, "updatedAt", NOW);
        return step;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(stepService.create(any())).thenReturn(makeStep(1, null));

        mockMvc.perform(
                        post("/api/v1/test-cases/{tcId}/steps", TEST_CASE_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "stepNumber": 1,
                                  "action": "Open login page",
                                  "expectedResult": "Page renders"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(STEP_ID.toString())))
                .andExpect(jsonPath("$.testCaseId", is(TEST_CASE_ID.toString())))
                .andExpect(jsonPath("$.stepNumber", is(1)))
                .andExpect(jsonPath("$.action", is("Open login page")))
                .andExpect(jsonPath("$.expectedResult", is("Page renders")))
                .andExpect(jsonPath("$.actualResult", is(nullValue())));
    }

    @Test
    void createAcceptsRichTextWithMarkdownImage() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(stepService.create(any())).thenReturn(makeStep(1, null));
        var captor = ArgumentCaptor.forClass(CreateTestCaseStepCommand.class);

        var richAction = "## Open page\n\n![login](https://example.com/login.png)";
        var richExpected = "Page renders **completely**";
        mockMvc.perform(
                        post("/api/v1/test-cases/{tcId}/steps", TEST_CASE_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "stepNumber": 1,
                                  "action": "## Open page\\n\\n![login](https://example.com/login.png)",
                                  "expectedResult": "Page renders **completely**"
                                }
                                """))
                .andExpect(status().isCreated());

        // Pin the controller's command assembly: rich-text fields must reach
        // the service exactly as the request body shaped them. Without the
        // captor a regression that mapped `action` to `expectedResult` (or
        // truncated the markdown) would still match `any()` and the test
        // would pass silently.
        verify(stepService).create(captor.capture());
        var cmd = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(cmd.action()).isEqualTo(richAction);
        org.assertj.core.api.Assertions.assertThat(cmd.expectedResult()).isEqualTo(richExpected);
        org.assertj.core.api.Assertions.assertThat(cmd.stepNumber()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(cmd.testCaseId()).isEqualTo(TEST_CASE_ID);
    }

    static Stream<Arguments> invalidCreateBodies() {
        return Stream.of(
                Arguments.of(
                        "missing-step-number",
                        """
                        {"action": "act", "expectedResult": "exp"}
                        """),
                Arguments.of(
                        "zero-step-number",
                        """
                        {"stepNumber": 0, "action": "act", "expectedResult": "exp"}
                        """),
                Arguments.of(
                        "negative-step-number",
                        """
                        {"stepNumber": -1, "action": "act", "expectedResult": "exp"}
                        """),
                Arguments.of(
                        "blank-action",
                        """
                        {"stepNumber": 1, "action": "", "expectedResult": "exp"}
                        """),
                Arguments.of(
                        "missing-action",
                        """
                        {"stepNumber": 1, "expectedResult": "exp"}
                        """),
                Arguments.of(
                        "blank-expected-result",
                        """
                        {"stepNumber": 1, "action": "act", "expectedResult": ""}
                        """),
                Arguments.of(
                        "missing-expected-result",
                        """
                        {"stepNumber": 1, "action": "act"}
                        """));
    }

    @ParameterizedTest(name = "create rejects {0} with 422")
    @MethodSource("invalidCreateBodies")
    void createRejectsInvalidBody(String label, String body) throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(post("/api/v1/test-cases/{tcId}/steps", TEST_CASE_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createRejectsOversizeAction() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        var oversize = "a".repeat(10_001);
        var body = "{\"stepNumber\": 1, \"action\": \"" + oversize + "\", \"expectedResult\": \"exp\"}";

        mockMvc.perform(post("/api/v1/test-cases/{tcId}/steps", TEST_CASE_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listReturnsStepsInOrder() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(stepService.listByTestCase(PROJECT_ID, TEST_CASE_ID))
                .thenReturn(List.of(makeStep(1, null), makeStep(2, "observed")));

        mockMvc.perform(get("/api/v1/test-cases/{tcId}/steps", TEST_CASE_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].stepNumber", is(1)))
                .andExpect(jsonPath("$[1].stepNumber", is(2)))
                .andExpect(jsonPath("$[1].actualResult", is("observed")));
    }

    @Test
    void getByIdReturnsStep() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(stepService.getById(PROJECT_ID, TEST_CASE_ID, STEP_ID)).thenReturn(makeStep(1, null));

        mockMvc.perform(get("/api/v1/test-cases/{tcId}/steps/{sId}", TEST_CASE_ID, STEP_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(STEP_ID.toString())));
    }

    @Test
    void updateReturnsUpdatedStep() throws Exception {
        var step = makeStep(3, null);
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(stepService.update(eq(PROJECT_ID), eq(TEST_CASE_ID), eq(STEP_ID), any()))
                .thenReturn(step);
        var captor = ArgumentCaptor.forClass(UpdateTestCaseStepCommand.class);

        mockMvc.perform(
                        put("/api/v1/test-cases/{tcId}/steps/{sId}", TEST_CASE_ID, STEP_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"stepNumber": 3, "action": "Open page", "clearActualResult": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stepNumber", is(3)));

        // Capture the command and assert the controller forwarded every field
        // from the request body. clearActualResult is the riskier one — a
        // controller that hardcoded it to false would still satisfy any() and
        // the wipe would never happen.
        verify(stepService).update(eq(PROJECT_ID), eq(TEST_CASE_ID), eq(STEP_ID), captor.capture());
        var cmd = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(cmd.stepNumber()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(cmd.action()).isEqualTo("Open page");
        org.assertj.core.api.Assertions.assertThat(cmd.clearActualResult()).isTrue();
    }

    @Test
    void updateRejectsNonPositiveStepNumber() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        put("/api/v1/test-cases/{tcId}/steps/{sId}", TEST_CASE_ID, STEP_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"stepNumber": 0}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/test-cases/{tcId}/steps/{sId}", TEST_CASE_ID, STEP_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(stepService).delete(PROJECT_ID, TEST_CASE_ID, STEP_ID);
    }
}
