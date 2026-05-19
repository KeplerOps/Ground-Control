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

import com.keplerops.groundcontrol.api.testcases.TestPlanController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestPlanCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestPlanService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestPlanCommand;
import com.keplerops.groundcontrol.domain.testcases.state.TestPlanStatus;
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
@WebMvcTest(TestPlanController.class)
class TestPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestPlanService testPlanService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLAN_ID = UUID.fromString("00000000-0000-0000-0000-000000000700");
    private static final Instant NOW = Instant.parse("2026-05-17T12:00:00Z");

    private TestPlan makePlan() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var plan = new TestPlan(project, "TP-001", "Wave-1 acceptance");
        plan.setDescription("scope notes");
        plan.setProduct("ground-control");
        plan.setVersion("1.2.0");
        plan.setBuild("build-42");
        plan.setStartDate(LocalDate.of(2026, 6, 1));
        plan.setEndDate(LocalDate.of(2026, 6, 30));
        setField(plan, "id", PLAN_ID);
        setField(plan, "createdAt", NOW);
        setField(plan, "updatedAt", NOW);
        return plan;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testPlanService.create(any(CreateTestPlanCommand.class))).thenReturn(makePlan());

        mockMvc.perform(
                        post("/api/v1/test-plans")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "uid": "TP-001",
                                  "name": "Wave-1 acceptance",
                                  "description": "scope notes",
                                  "product": "ground-control",
                                  "version": "1.2.0",
                                  "build": "build-42",
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.uid", is("TP-001")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.startDate", is("2026-06-01")));

        // Capture the command so a body-binding regression (wrong field name,
        // mistyped date binding, dropped product/version/build) surfaces at
        // the unit level instead of slipping past any() into the service.
        ArgumentCaptor<CreateTestPlanCommand> captor = ArgumentCaptor.forClass(CreateTestPlanCommand.class);
        verify(testPlanService).create(captor.capture());
        var cmd = captor.getValue();
        assertThat(cmd.uid()).isEqualTo("TP-001");
        assertThat(cmd.name()).isEqualTo("Wave-1 acceptance");
        assertThat(cmd.description()).isEqualTo("scope notes");
        assertThat(cmd.product()).isEqualTo("ground-control");
        assertThat(cmd.version()).isEqualTo("1.2.0");
        assertThat(cmd.build()).isEqualTo("build-42");
        assertThat(cmd.startDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(cmd.endDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void createReturns422WhenUidBlank() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(post("/api/v1/test-plans")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uid\":\"\",\"name\":\"Plan\"}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(testPlanService);
    }

    @Test
    void createReturns422WhenNameBlank() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(post("/api/v1/test-plans")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uid\":\"TP-001\",\"name\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(testPlanService);
    }

    @Test
    void createReturns422WhenNameTooLong() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        String tooLong = "x".repeat(201);
        mockMvc.perform(post("/api/v1/test-plans")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uid\":\"TP-001\",\"name\":\"" + tooLong + "\"}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(testPlanService);
    }

    @Test
    void listReturnsPlans() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testPlanService.listByProject(PROJECT_ID)).thenReturn(List.of(makePlan()));

        mockMvc.perform(get("/api/v1/test-plans").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("TP-001")));
    }

    @Test
    void getByIdReturnsPlan() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testPlanService.getById(PROJECT_ID, PLAN_ID)).thenReturn(makePlan());

        mockMvc.perform(get("/api/v1/test-plans/{id}", PLAN_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("TP-001")))
                .andExpect(jsonPath("$.name", is("Wave-1 acceptance")));
    }

    @Test
    void getByUidReturnsPlan() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testPlanService.getByUid(PROJECT_ID, "TP-001")).thenReturn(makePlan());

        mockMvc.perform(get("/api/v1/test-plans/uid/{uid}", "TP-001").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("TP-001")));
    }

    @Test
    void updateBindsAllFieldsAndClearFlags() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(testPlanService.update(eq(PROJECT_ID), eq(PLAN_ID), any(UpdateTestPlanCommand.class)))
                .thenReturn(makePlan());

        mockMvc.perform(
                        put("/api/v1/test-plans/{id}", PLAN_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "name": "Renamed",
                                  "description": "new",
                                  "product": "new-product",
                                  "version": "2.0.0",
                                  "build": "new-build",
                                  "startDate": "2026-07-01",
                                  "endDate": "2026-07-31",
                                  "clearDescription": false,
                                  "clearProduct": false,
                                  "clearVersion": false,
                                  "clearBuild": true,
                                  "clearStartDate": false,
                                  "clearEndDate": false
                                }
                                """))
                .andExpect(status().isOk())
                // Pass-through assertion — values come from the mocked plan
                // returned by makePlan(), so the response body must reflect
                // the service result. A regression where the controller
                // returns the request body, a fresh entity, or null would
                // pass the andExpect(isOk()) check but fail here
                // (test-quality cycle 1).
                .andExpect(jsonPath("$.uid", is("TP-001")))
                .andExpect(jsonPath("$.name", is("Wave-1 acceptance")))
                .andExpect(jsonPath("$.build", is("build-42")))
                .andExpect(jsonPath("$.startDate", is("2026-06-01")));
        // Capture so wrong DTO field names / dropped clear-flag bindings
        // surface at the unit level — addresses the TC-005 test-quality
        // cycle-1 lesson.
        ArgumentCaptor<UpdateTestPlanCommand> captor = ArgumentCaptor.forClass(UpdateTestPlanCommand.class);
        verify(testPlanService).update(eq(PROJECT_ID), eq(PLAN_ID), captor.capture());
        var cmd = captor.getValue();
        assertThat(cmd.name()).isEqualTo("Renamed");
        assertThat(cmd.description()).isEqualTo("new");
        assertThat(cmd.product()).isEqualTo("new-product");
        assertThat(cmd.version()).isEqualTo("2.0.0");
        // build was explicitly cleared — the client supplied "new-build"
        // AND clearBuild=true; the service contract treats the clear flag
        // as authoritative, so the captured `clearBuild` is true.
        assertThat(cmd.clearBuild()).isTrue();
        assertThat(cmd.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(cmd.endDate()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void transitionStatusBindsRequestStatus() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        var transitioned = makePlan();
        setField(transitioned, "status", TestPlanStatus.ACTIVE);
        when(testPlanService.transitionStatus(PROJECT_ID, PLAN_ID, TestPlanStatus.ACTIVE))
                .thenReturn(transitioned);

        mockMvc.perform(put("/api/v1/test-plans/{id}/status", PLAN_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
        verify(testPlanService).transitionStatus(PROJECT_ID, PLAN_ID, TestPlanStatus.ACTIVE);
    }

    @Test
    void transitionStatusReturns422WhenStatusMissing() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(put("/api/v1/test-plans/{id}/status", PLAN_ID)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(testPlanService);
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/test-plans/{id}", PLAN_ID).param("project", "ground-control"))
                .andExpect(status().isNoContent());
        verify(testPlanService).delete(PROJECT_ID, PLAN_ID);
    }
}
