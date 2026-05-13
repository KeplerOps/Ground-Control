package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.controls.ControlTestController;
import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlTest;
import com.keplerops.groundcontrol.domain.controls.service.ControlTestService;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.time.LocalDate;
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
@WebMvcTest(ControlTestController.class)
class ControlTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ControlTestService controlTestService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CONTROL_ID = UUID.fromString("00000000-0000-0000-0000-000000000500");
    private static final UUID TEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000600");
    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

    private ControlTest makeControlTest() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var control = new Control(project, "CTRL-001", "Access Control", ControlFunction.PREVENTIVE);
        setField(control, "id", CONTROL_ID);
        var ct = new ControlTest(
                project,
                control,
                "CT-001",
                ControlTestMethodology.INSPECTION,
                "Inspect access logs.",
                "No unauthorized attempts.",
                "0 unauthorized attempts.",
                ControlTestConclusion.EFFECTIVE,
                "auditor@example.com",
                LocalDate.of(2026, 5, 1));
        setField(ct, "id", TEST_ID);
        setField(ct, "createdAt", NOW);
        setField(ct, "updatedAt", NOW);
        return ct;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlTestService.create(any())).thenReturn(makeControlTest());

        mockMvc.perform(
                        post("/api/v1/control-tests")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "controlId": "00000000-0000-0000-0000-000000000500",
                                          "uid": "CT-001",
                                          "methodology": "INSPECTION",
                                          "testSteps": "Inspect access logs.",
                                          "expectedResults": "No unauthorized attempts.",
                                          "actualResults": "0 unauthorized attempts.",
                                          "conclusion": "EFFECTIVE",
                                          "testerIdentity": "auditor@example.com",
                                          "testDate": "2026-05-01"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(TEST_ID.toString())))
                .andExpect(jsonPath("$.uid", is("CT-001")))
                .andExpect(jsonPath("$.controlId", is(CONTROL_ID.toString())))
                .andExpect(jsonPath("$.methodology", is("INSPECTION")))
                .andExpect(jsonPath("$.conclusion", is("EFFECTIVE")));
    }

    @Test
    void createReturns422WhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/control-tests")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "uid": "CT-001",
                                          "methodology": "INSPECTION",
                                          "testSteps": "Inspect.",
                                          "expectedResults": "None.",
                                          "actualResults": "None.",
                                          "conclusion": "EFFECTIVE",
                                          "testerIdentity": "auditor",
                                          "testDate": "2026-05-01"
                                        }
                                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("validation_error")));
    }

    @Test
    void createReturns422WhenInvalidMethodologyEnum() throws Exception {
        mockMvc.perform(
                        post("/api/v1/control-tests")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "controlId": "00000000-0000-0000-0000-000000000500",
                                          "uid": "CT-001",
                                          "methodology": "NOT_A_METHOD",
                                          "testSteps": "x",
                                          "expectedResults": "y",
                                          "actualResults": "z",
                                          "conclusion": "EFFECTIVE",
                                          "testerIdentity": "auditor",
                                          "testDate": "2026-05-01"
                                        }
                                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("validation_error")))
                .andExpect(jsonPath("$.error.detail.field", is("methodology")));
    }

    @Test
    void listReturnsTests() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlTestService.listByProject(PROJECT_ID)).thenReturn(List.of(makeControlTest()));

        mockMvc.perform(get("/api/v1/control-tests").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("CT-001")));
    }

    @Test
    void listByControlIdFilters() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlTestService.listByProjectAndControl(PROJECT_ID, CONTROL_ID)).thenReturn(List.of(makeControlTest()));

        mockMvc.perform(get("/api/v1/control-tests")
                        .param("project", "ground-control")
                        .param("controlId", CONTROL_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getByIdReturnsTest() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlTestService.getById(PROJECT_ID, TEST_ID)).thenReturn(makeControlTest());

        mockMvc.perform(get("/api/v1/control-tests/{id}", TEST_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("CT-001")));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlTestService.getById(PROJECT_ID, TEST_ID))
                .thenThrow(new NotFoundException("ControlTest not found: " + TEST_ID));

        mockMvc.perform(get("/api/v1/control-tests/{id}", TEST_ID).param("project", "ground-control"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAppliesProvidedFields() throws Exception {
        var updated = makeControlTest();
        updated.setConclusion(ControlTestConclusion.INEFFECTIVE);
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(controlTestService.update(eq(PROJECT_ID), eq(TEST_ID), any())).thenReturn(updated);

        mockMvc.perform(
                        put("/api/v1/control-tests/{id}", TEST_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "conclusion": "INEFFECTIVE" }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conclusion", is("INEFFECTIVE")));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/control-tests/{id}", TEST_ID).param("project", "ground-control"))
                .andExpect(status().isNoContent());
    }
}
