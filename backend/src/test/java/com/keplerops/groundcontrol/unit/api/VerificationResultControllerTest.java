package com.keplerops.groundcontrol.unit.api;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.verification.VerificationResultController;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.verification.model.VerificationResult;
import com.keplerops.groundcontrol.domain.verification.service.VerificationResultService;
import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VerificationResultController.class)
class VerificationResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationResultService verificationResultService;

    @MockitoBean
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VR_ID = UUID.fromString("00000000-0000-0000-0000-000000000600");
    private static final Instant NOW = Instant.parse("2026-04-05T12:00:00Z");

    private VerificationResult makeResult() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        var vr = new VerificationResult(project, "openjml-esc", VerificationStatus.PROVEN, AssuranceLevel.L1, NOW);
        vr.setProperty("requires x > 0");
        setField(vr, "id", VR_ID);
        setField(vr, "createdAt", NOW);
        setField(vr, "updatedAt", NOW);
        return vr;
    }

    @Test
    void createReturns201() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(verificationResultService.create(any())).thenReturn(makeResult());

        mockMvc.perform(
                        post("/api/v1/verification-results")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "prover": "openjml-esc",
                                  "property": "requires x > 0",
                                  "result": "PROVEN",
                                  "assuranceLevel": "L1",
                                  "verifiedAt": "2026-04-05T12:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(VR_ID.toString())))
                .andExpect(jsonPath("$.prover", is("openjml-esc")))
                .andExpect(jsonPath("$.result", is("PROVEN")))
                .andExpect(jsonPath("$.assuranceLevel", is("L1")))
                .andExpect(jsonPath("$.targetId").value(nullValue()))
                .andExpect(jsonPath("$.requirementId").value(nullValue()));
    }

    @Test
    void listReturnsResults() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(verificationResultService.listByProject(eq(PROJECT_ID), isNull(), isNull(), isNull()))
                .thenReturn(List.of(makeResult()));

        mockMvc.perform(get("/api/v1/verification-results").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].prover", is("openjml-esc")));
    }

    @Test
    void getByIdReturnsResult() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(verificationResultService.getById(PROJECT_ID, VR_ID)).thenReturn(makeResult());

        mockMvc.perform(get("/api/v1/verification-results/{id}", VR_ID).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prover", is("openjml-esc")))
                .andExpect(jsonPath("$.result", is("PROVEN")));
    }

    @Test
    void updateReturnsUpdatedResult() throws Exception {
        var vr = makeResult();
        vr.setProver("tlaplus-tlc");
        vr.setResult(VerificationStatus.REFUTED);
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(verificationResultService.update(eq(PROJECT_ID), eq(VR_ID), any())).thenReturn(vr);

        mockMvc.perform(
                        put("/api/v1/verification-results/{id}", VR_ID)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"prover":"tlaplus-tlc","result":"REFUTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prover", is("tlaplus-tlc")))
                .andExpect(jsonPath("$.result", is("REFUTED")));
    }

    @Test
    void deleteReturns204() throws Exception {
        when(projectService.requireProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/verification-results/{id}", VR_ID).param("project", "ground-control"))
                .andExpect(status().isNoContent());

        verify(verificationResultService).delete(PROJECT_ID, VR_ID);
    }

    @Test
    void createReturns422WhenProverMissing() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);

        mockMvc.perform(
                        post("/api/v1/verification-results")
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "result": "PROVEN",
                                  "assuranceLevel": "L1",
                                  "verifiedAt": "2026-04-05T12:00:00Z"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
