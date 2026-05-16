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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class TestCaseControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Map<String, Object> validRequest(String uid) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("uid", uid);
        request.put("title", "Login flow validation");
        request.put("type", "MANUAL");
        request.put("priority", "HIGH");
        request.put("description", "# Verify a user can log in");
        request.put("preconditions", "- account exists\n- IdP reachable");
        request.put("postconditions", "- user lands on dashboard");
        request.put("estimatedDurationSeconds", 300);
        return request;
    }

    @Test
    void create_get_list_update_transition_delete_roundTrip() throws Exception {
        // Create
        var createResult = mockMvc.perform(post("/api/v1/test-cases")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("TC-INT-001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid", is("TC-INT-001")))
                .andExpect(jsonPath("$.type", is("MANUAL")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.estimatedDurationSeconds", is(300)))
                .andReturn();
        String testCaseId = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        // Duplicate UID rejected
        mockMvc.perform(post("/api/v1/test-cases")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("TC-INT-001"))))
                .andExpect(status().isConflict());

        // Get by id
        mockMvc.perform(get("/api/v1/test-cases/{id}", testCaseId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("TC-INT-001")));

        // Get by uid
        mockMvc.perform(get("/api/v1/test-cases/uid/{uid}", "TC-INT-001").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testCaseId)));

        // List
        mockMvc.perform(get("/api/v1/test-cases").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("TC-INT-001")));

        // Update — type + priority + duration; title intentionally not changed.
        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("type", "AUTOMATED");
        updateBody.put("priority", "CRITICAL");
        updateBody.put("estimatedDurationSeconds", 900);
        mockMvc.perform(put("/api/v1/test-cases/{id}", testCaseId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("AUTOMATED")))
                .andExpect(jsonPath("$.priority", is("CRITICAL")))
                .andExpect(jsonPath("$.estimatedDurationSeconds", is(900)))
                .andExpect(jsonPath("$.title", is("Login flow validation")));

        // Transition status DRAFT -> APPROVED
        mockMvc.perform(
                        put("/api/v1/test-cases/{id}/status", testCaseId)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"status":"APPROVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // Invalid transition APPROVED -> DRAFT — domain validation rejection
        mockMvc.perform(
                        put("/api/v1/test-cases/{id}/status", testCaseId)
                                .param("project", "ground-control")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"status":"DRAFT"}
                                """))
                .andExpect(status().isUnprocessableEntity());

        // Delete
        mockMvc.perform(delete("/api/v1/test-cases/{id}", testCaseId).param("project", "ground-control"))
                .andExpect(status().isNoContent());

        // Subsequent GET returns 404
        mockMvc.perform(get("/api/v1/test-cases/{id}", testCaseId).param("project", "ground-control"))
                .andExpect(status().isNotFound());
    }
}
