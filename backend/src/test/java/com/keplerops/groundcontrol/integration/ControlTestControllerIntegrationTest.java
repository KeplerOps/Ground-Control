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
class ControlTestControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String controlId;

    @BeforeEach
    void setupControl() throws Exception {
        Map<String, Object> controlRequest = Map.of(
                "uid", "CTRL-INT-001",
                "title", "Integration Test Control",
                "controlFunction", "PREVENTIVE");
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
        request.put("methodology", "INSPECTION");
        request.put("testSteps", "Inspect access logs.");
        request.put("expectedResults", "No unauthorized attempts.");
        request.put("actualResults", "0 unauthorized attempts.");
        request.put("conclusion", "EFFECTIVE");
        request.put("testerIdentity", "auditor@example.com");
        request.put("testDate", "2026-05-01");
        return request;
    }

    @Test
    void create_thenGet_thenList_thenUpdate_thenDelete() throws Exception {
        // Create
        var createResult = mockMvc.perform(post("/api/v1/control-tests")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("CT-INT-001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid", is("CT-INT-001")))
                .andExpect(jsonPath("$.controlId", is(controlId)))
                .andExpect(jsonPath("$.controlUid", is("CTRL-INT-001")))
                .andExpect(jsonPath("$.methodology", is("INSPECTION")))
                .andExpect(jsonPath("$.conclusion", is("EFFECTIVE")))
                .andReturn();
        String testId = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        // Get by id
        mockMvc.perform(get("/api/v1/control-tests/{id}", testId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("CT-INT-001")));

        // List filtered by controlId
        mockMvc.perform(get("/api/v1/control-tests")
                        .param("project", "ground-control")
                        .param("controlId", controlId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("CT-INT-001")));

        // Update conclusion
        mockMvc.perform(put("/api/v1/control-tests/{id}", testId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"conclusion\": \"INEFFECTIVE\", \"notes\": \"Re-tested after incident\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conclusion", is("INEFFECTIVE")))
                .andExpect(jsonPath("$.notes", is("Re-tested after incident")));

        // Delete
        mockMvc.perform(delete("/api/v1/control-tests/{id}", testId).param("project", "ground-control"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/control-tests/{id}", testId).param("project", "ground-control"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_rejectsDuplicateUid() throws Exception {
        mockMvc.perform(post("/api/v1/control-tests")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("CT-INT-DUP"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/control-tests")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("CT-INT-DUP"))))
                .andExpect(status().isConflict());
    }

    @Test
    void create_rejectsInvalidMethodologyEnumWith422() throws Exception {
        Map<String, Object> bad = new LinkedHashMap<>(validRequest("CT-INT-ENUM"));
        bad.put("methodology", "NOT_A_METHOD");
        mockMvc.perform(post("/api/v1/control-tests")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("validation_error")))
                .andExpect(jsonPath("$.error.detail.field", is("methodology")));
    }
}
