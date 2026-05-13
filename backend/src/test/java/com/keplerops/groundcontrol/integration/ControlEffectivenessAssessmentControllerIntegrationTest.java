package com.keplerops.groundcontrol.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class ControlEffectivenessAssessmentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String controlId;

    @BeforeEach
    void setupControl() throws Exception {
        Map<String, Object> controlRequest = Map.of(
                "uid", "CTRL-INT-002",
                "title", "Effectiveness Integration Control",
                "controlFunction", "DETECTIVE");
        var result = mockMvc.perform(post("/api/v1/controls")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(controlRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        controlId = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    private Map<String, Object> validRequest(String uid) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("controlId", controlId);
        request.put("uid", uid);
        request.put("designEffectiveness", "EFFECTIVE");
        request.put("operatingEffectiveness", "PARTIALLY_EFFECTIVE");
        request.put("assessedAt", "2026-05-01");
        request.put("assessor", "auditor@example.com");
        request.put("rationale", "Design solid; one operating gap.");
        return request;
    }

    @Test
    void create_thenGet_thenList_thenUpdate_thenDelete() throws Exception {
        var createResult = mockMvc.perform(post("/api/v1/control-effectiveness-assessments")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("CEA-INT-001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid", is("CEA-INT-001")))
                .andExpect(jsonPath("$.designEffectiveness", is("EFFECTIVE")))
                .andExpect(jsonPath("$.operatingEffectiveness", is("PARTIALLY_EFFECTIVE")))
                .andReturn();
        String id = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/v1/control-effectiveness-assessments/{id}", id)
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("CEA-INT-001")));

        mockMvc.perform(get("/api/v1/control-effectiveness-assessments")
                        .param("project", "ground-control")
                        .param("controlId", controlId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(put("/api/v1/control-effectiveness-assessments/{id}", id)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"operatingEffectiveness\": \"INEFFECTIVE\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatingEffectiveness", is("INEFFECTIVE")));

        mockMvc.perform(delete("/api/v1/control-effectiveness-assessments/{id}", id)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_rejectsDuplicateUid() throws Exception {
        mockMvc.perform(post("/api/v1/control-effectiveness-assessments")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("CEA-INT-DUP"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/control-effectiveness-assessments")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("CEA-INT-DUP"))))
                .andExpect(status().isConflict());
    }

    @Test
    void create_rejectsInvalidRatingEnumWith422() throws Exception {
        Map<String, Object> bad = new LinkedHashMap<>(validRequest("CEA-INT-ENUM"));
        bad.put("designEffectiveness", "MAYBE");
        mockMvc.perform(post("/api/v1/control-effectiveness-assessments")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("validation_error")))
                .andExpect(jsonPath("$.error.detail.field", is("designEffectiveness")));
    }
}
