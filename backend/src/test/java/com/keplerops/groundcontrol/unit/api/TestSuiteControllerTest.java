package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.testcases.TestSuiteController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuite;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuiteMember;
import com.keplerops.groundcontrol.domain.testcases.model.TestSuiteSourceRequirement;
import com.keplerops.groundcontrol.domain.testcases.service.AddTestSuiteMemberCommand;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestSuiteCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestSuiteService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestSuiteCommand;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
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
@WebMvcTest(TestSuiteController.class)
class TestSuiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestSuiteService testSuiteService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUITE_ID = UUID.fromString("00000000-0000-0000-0000-000000000800");
    private static final UUID TC_ID = UUID.fromString("00000000-0000-0000-0000-000000000801");
    private static final UUID REQ_ID = UUID.fromString("00000000-0000-0000-0000-000000000802");
    private static final Instant NOW = Instant.parse("2026-05-17T12:00:00Z");

    private Project project() {
        var p = new Project("ground-control", "Ground Control");
        setField(p, "id", PROJECT_ID);
        return p;
    }

    private TestSuite suite() {
        var s = new TestSuite(project(), "TS-001", "Wave-1 selection", TestSuitePopulationMode.STATIC);
        s.setDescription("notes");
        setField(s, "id", SUITE_ID);
        setField(s, "createdAt", NOW);
        setField(s, "updatedAt", NOW);
        return s;
    }

    private TestCase testCase() {
        var tc = new TestCase(project(), "TC-001", "title", TestCaseType.MANUAL, TestCasePriority.MEDIUM);
        setField(tc, "id", TC_ID);
        setField(tc, "createdAt", NOW);
        setField(tc, "updatedAt", NOW);
        return tc;
    }

    private Requirement requirement() {
        var req = new Requirement(project(), "REQ-001", "title", "statement");
        setField(req, "id", REQ_ID);
        return req;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testSuiteService.create(any(CreateTestSuiteCommand.class))).thenReturn(suite());

        mockMvc.perform(
                        post("/api/v1/test-suites")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "uid": "TS-001",
                                  "name": "Wave-1 selection",
                                  "description": "notes",
                                  "populationMode": "STATIC"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.uid", is("TS-001")))
                .andExpect(jsonPath("$.populationMode", is("STATIC")))
                .andExpect(jsonPath("$.description", is("notes")));

        ArgumentCaptor<CreateTestSuiteCommand> captor = ArgumentCaptor.forClass(CreateTestSuiteCommand.class);
        verify(testSuiteService).create(captor.capture());
        var cmd = captor.getValue();
        assertThat(cmd.uid()).isEqualTo("TS-001");
        assertThat(cmd.name()).isEqualTo("Wave-1 selection");
        assertThat(cmd.description()).isEqualTo("notes");
        assertThat(cmd.populationMode()).isEqualTo(TestSuitePopulationMode.STATIC);
        assertThat(cmd.criteria()).isNotNull();
        assertThat(cmd.criteria().hasAny()).isFalse();
    }

    @Test
    void createBindsQueryBasedCriteria() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        var queryBased = new TestSuite(project(), "TS-Q-001", "n", TestSuitePopulationMode.QUERY_BASED);
        queryBased.setCriteriaStatus(TestCaseStatus.APPROVED);
        setField(queryBased, "id", SUITE_ID);
        setField(queryBased, "createdAt", NOW);
        setField(queryBased, "updatedAt", NOW);
        when(testSuiteService.create(any(CreateTestSuiteCommand.class))).thenReturn(queryBased);

        mockMvc.perform(
                        post("/api/v1/test-suites")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "uid": "TS-Q-001",
                                  "name": "n",
                                  "populationMode": "QUERY_BASED",
                                  "criteriaStatus": "APPROVED",
                                  "criteriaType": "AUTOMATED",
                                  "criteriaPriority": "HIGH",
                                  "criteriaTextSearch": "payment"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.criteriaStatus", is("APPROVED")));

        ArgumentCaptor<CreateTestSuiteCommand> captor = ArgumentCaptor.forClass(CreateTestSuiteCommand.class);
        verify(testSuiteService).create(captor.capture());
        var cmd = captor.getValue();
        assertThat(cmd.populationMode()).isEqualTo(TestSuitePopulationMode.QUERY_BASED);
        assertThat(cmd.criteria().status()).isEqualTo(TestCaseStatus.APPROVED);
        assertThat(cmd.criteria().type()).isEqualTo(TestCaseType.AUTOMATED);
        assertThat(cmd.criteria().priority()).isEqualTo(TestCasePriority.HIGH);
        assertThat(cmd.criteria().textSearch()).isEqualTo("payment");
    }

    @Test
    void createReturns422WhenUidBlank() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(post("/api/v1/test-suites")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uid\":\"\",\"name\":\"n\",\"populationMode\":\"STATIC\"}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(testSuiteService);
    }

    @Test
    void createReturns422WhenPopulationModeMissing() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(post("/api/v1/test-suites")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uid\":\"TS-001\",\"name\":\"n\"}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(testSuiteService);
    }

    @Test
    void listReturnsSuites() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testSuiteService.listByProject(PROJECT_ID)).thenReturn(List.of(suite()));

        mockMvc.perform(get("/api/v1/test-suites").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("TS-001")));
    }

    @Test
    void getByIdReturnsSuite() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testSuiteService.getById(PROJECT_ID, SUITE_ID)).thenReturn(suite());

        mockMvc.perform(get("/api/v1/test-suites/{id}", SUITE_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("TS-001")));
    }

    @Test
    void getByUidReturnsSuite() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testSuiteService.getByUid(PROJECT_ID, "TS-001")).thenReturn(suite());

        mockMvc.perform(get("/api/v1/test-suites/uid/{uid}", "TS-001").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("TS-001")));
    }

    @Test
    void updateBindsAllFieldsAndClearFlags() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testSuiteService.update(eq(PROJECT_ID), eq(SUITE_ID), any(UpdateTestSuiteCommand.class)))
                .thenReturn(suite());

        mockMvc.perform(
                        put("/api/v1/test-suites/{id}", SUITE_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Renamed",
                                  "description": "new",
                                  "clearDescription": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("TS-001")));

        ArgumentCaptor<UpdateTestSuiteCommand> captor = ArgumentCaptor.forClass(UpdateTestSuiteCommand.class);
        verify(testSuiteService).update(eq(PROJECT_ID), eq(SUITE_ID), captor.capture());
        var cmd = captor.getValue();
        assertThat(cmd.name()).isEqualTo("Renamed");
        assertThat(cmd.description()).isEqualTo("new");
        assertThat(cmd.clearDescription()).isTrue();
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/test-suites/{id}", SUITE_ID).param("project", "ground-control"))
                .andExpect(status().isNoContent());
        verify(testSuiteService).delete(PROJECT_ID, SUITE_ID);
    }

    @Test
    void resolveTestCasesReturnsResolutionList() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testSuiteService.resolveTestCases(PROJECT_ID, SUITE_ID)).thenReturn(List.of(testCase()));

        mockMvc.perform(get("/api/v1/test-suites/{id}/test-cases", SUITE_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("TC-001")));
    }

    @Test
    void addMemberCreates201() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        var member = new TestSuiteMember(suite(), testCase(), 0);
        setField(member, "id", UUID.randomUUID());
        setField(member, "createdAt", NOW);
        setField(member, "updatedAt", NOW);
        when(testSuiteService.addMember(eq(PROJECT_ID), eq(SUITE_ID), any(AddTestSuiteMemberCommand.class)))
                .thenReturn(member);

        mockMvc.perform(post("/api/v1/test-suites/{id}/members", SUITE_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testCaseId\":\"" + TC_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.testCaseId", is(TC_ID.toString())))
                .andExpect(jsonPath("$.testCaseUid", is("TC-001")))
                .andExpect(jsonPath("$.position", is(0)));
    }

    @Test
    void addMemberReturns422WhenTestCaseIdMissing() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(post("/api/v1/test-suites/{id}/members", SUITE_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(testSuiteService);
    }

    @Test
    void removeMemberReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/test-suites/{id}/members/{tcId}", SUITE_ID, TC_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());
        verify(testSuiteService).removeMember(PROJECT_ID, SUITE_ID, TC_ID);
    }

    @Test
    void listMembersReturnsMembersInOrder() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        var member = new TestSuiteMember(suite(), testCase(), 0);
        setField(member, "id", UUID.randomUUID());
        setField(member, "createdAt", NOW);
        setField(member, "updatedAt", NOW);
        when(testSuiteService.listMembers(PROJECT_ID, SUITE_ID)).thenReturn(List.of(member));

        mockMvc.perform(get("/api/v1/test-suites/{id}/members", SUITE_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].testCaseUid", is("TC-001")))
                .andExpect(jsonPath("$[0].position", is(0)));
        verify(testSuiteService).listMembers(PROJECT_ID, SUITE_ID);
    }

    @Test
    void listSourceRequirementsReturnsSources() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        var reqSuite = new TestSuite(project(), "TS-R-001", "n", TestSuitePopulationMode.REQUIREMENTS_BASED);
        setField(reqSuite, "id", SUITE_ID);
        var source = new TestSuiteSourceRequirement(reqSuite, requirement());
        setField(source, "id", UUID.randomUUID());
        setField(source, "createdAt", NOW);
        setField(source, "updatedAt", NOW);
        when(testSuiteService.listSourceRequirements(PROJECT_ID, SUITE_ID)).thenReturn(List.of(source));

        mockMvc.perform(get("/api/v1/test-suites/{id}/source-requirements", SUITE_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requirementUid", is("REQ-001")));
        verify(testSuiteService).listSourceRequirements(PROJECT_ID, SUITE_ID);
    }

    @Test
    void addSourceRequirementCreates201() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        var reqSuite = new TestSuite(project(), "TS-R-001", "n", TestSuitePopulationMode.REQUIREMENTS_BASED);
        setField(reqSuite, "id", SUITE_ID);
        var source = new TestSuiteSourceRequirement(reqSuite, requirement());
        setField(source, "id", UUID.randomUUID());
        setField(source, "createdAt", NOW);
        setField(source, "updatedAt", NOW);
        when(testSuiteService.addSourceRequirement(PROJECT_ID, SUITE_ID, REQ_ID))
                .thenReturn(source);

        mockMvc.perform(post("/api/v1/test-suites/{id}/source-requirements", SUITE_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementId\":\"" + REQ_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requirementId", is(REQ_ID.toString())))
                .andExpect(jsonPath("$.requirementUid", is("REQ-001")));
    }
}
