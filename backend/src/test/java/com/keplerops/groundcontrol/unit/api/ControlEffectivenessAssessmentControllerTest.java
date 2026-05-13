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

import com.keplerops.groundcontrol.api.controls.ControlEffectivenessAssessmentController;
import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment;
import com.keplerops.groundcontrol.domain.controls.service.ControlEffectivenessAssessmentService;
import com.keplerops.groundcontrol.domain.controls.state.ControlEffectivenessRating;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
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
@WebMvcTest(ControlEffectivenessAssessmentController.class)
class ControlEffectivenessAssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ControlEffectivenessAssessmentService service;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CONTROL_ID = UUID.fromString("00000000-0000-0000-0000-000000000500");
    private static final UUID ASSESSMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000700");
    private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

    private ControlEffectivenessAssessment makeAssessment() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var control = new Control(project, "CTRL-001", "Access Control", ControlFunction.PREVENTIVE);
        setField(control, "id", CONTROL_ID);
        var assessment = new ControlEffectivenessAssessment(
                project,
                control,
                "CEA-001",
                ControlEffectivenessRating.EFFECTIVE,
                ControlEffectivenessRating.PARTIALLY_EFFECTIVE,
                LocalDate.of(2026, 5, 1),
                "auditor@example.com");
        assessment.setRationale("Design solid; one operating gap.");
        setField(assessment, "id", ASSESSMENT_ID);
        setField(assessment, "createdAt", NOW);
        setField(assessment, "updatedAt", NOW);
        return assessment;
    }

    @Test
    void createReturns201WithBothRatings() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(service.create(any())).thenReturn(makeAssessment());

        mockMvc.perform(
                        post("/api/v1/control-effectiveness-assessments")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "controlId": "00000000-0000-0000-0000-000000000500",
                                          "uid": "CEA-001",
                                          "designEffectiveness": "EFFECTIVE",
                                          "operatingEffectiveness": "PARTIALLY_EFFECTIVE",
                                          "assessedAt": "2026-05-01",
                                          "assessor": "auditor@example.com",
                                          "rationale": "Design solid; one operating gap."
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(ASSESSMENT_ID.toString())))
                .andExpect(jsonPath("$.designEffectiveness", is("EFFECTIVE")))
                .andExpect(jsonPath("$.operatingEffectiveness", is("PARTIALLY_EFFECTIVE")));
    }

    @Test
    void createReturns422WhenRatingMissing() throws Exception {
        mockMvc.perform(
                        post("/api/v1/control-effectiveness-assessments")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "controlId": "00000000-0000-0000-0000-000000000500",
                                          "uid": "CEA-001",
                                          "designEffectiveness": "EFFECTIVE",
                                          "assessedAt": "2026-05-01",
                                          "assessor": "auditor@example.com"
                                        }
                                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("validation_error")));
    }

    @Test
    void createReturns422WhenInvalidRatingEnum() throws Exception {
        mockMvc.perform(
                        post("/api/v1/control-effectiveness-assessments")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "controlId": "00000000-0000-0000-0000-000000000500",
                                          "uid": "CEA-001",
                                          "designEffectiveness": "MAYBE",
                                          "operatingEffectiveness": "EFFECTIVE",
                                          "assessedAt": "2026-05-01",
                                          "assessor": "auditor@example.com"
                                        }
                                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.detail.field", is("designEffectiveness")))
                .andExpect(jsonPath("$.error.detail.validValues").exists());
    }

    @Test
    void listReturnsAssessments() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(service.listByProject(PROJECT_ID)).thenReturn(List.of(makeAssessment()));

        mockMvc.perform(get("/api/v1/control-effectiveness-assessments").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("CEA-001")));
    }

    @Test
    void listByControlIdFilters() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(service.listByProjectAndControl(PROJECT_ID, CONTROL_ID)).thenReturn(List.of(makeAssessment()));

        mockMvc.perform(get("/api/v1/control-effectiveness-assessments")
                        .param("project", "ground-control")
                        .param("controlId", CONTROL_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(service.getById(PROJECT_ID, ASSESSMENT_ID))
                .thenThrow(new NotFoundException("ControlEffectivenessAssessment not found: " + ASSESSMENT_ID));

        mockMvc.perform(get("/api/v1/control-effectiveness-assessments/{id}", ASSESSMENT_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAppliesProvidedFields() throws Exception {
        var updated = makeAssessment();
        updated.setOperatingEffectiveness(ControlEffectivenessRating.INEFFECTIVE);
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(service.update(eq(PROJECT_ID), eq(ASSESSMENT_ID), any())).thenReturn(updated);

        mockMvc.perform(
                        put("/api/v1/control-effectiveness-assessments/{id}", ASSESSMENT_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        { "operatingEffectiveness": "INEFFECTIVE" }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatingEffectiveness", is("INEFFECTIVE")));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/control-effectiveness-assessments/{id}", ASSESSMENT_ID)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());
    }
}
