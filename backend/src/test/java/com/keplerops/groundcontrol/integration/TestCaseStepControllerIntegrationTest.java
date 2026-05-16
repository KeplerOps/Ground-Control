package com.keplerops.groundcontrol.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
class TestCaseStepControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Map<String, Object> testCaseRequest(String uid) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("uid", uid);
        request.put("title", "Login flow validation");
        request.put("type", "MANUAL");
        request.put("priority", "HIGH");
        return request;
    }

    private static Map<String, Object> stepRequest(int stepNumber, String action, String expected) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("stepNumber", stepNumber);
        request.put("action", action);
        request.put("expectedResult", expected);
        return request;
    }

    private String createTestCase(String uid) throws Exception {
        var result = mockMvc.perform(post("/api/v1/test-cases")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCaseRequest(uid))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    private String createStep(String testCaseId, Map<String, Object> body) throws Exception {
        var result = mockMvc.perform(post("/api/v1/test-cases/{tc}/steps", testCaseId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    @Test
    void create_list_update_delete_roundTrip() throws Exception {
        var testCaseId = createTestCase("TC-STEP-001");

        // Create three steps out of order to verify list sorts by stepNumber
        createStep(testCaseId, stepRequest(2, "Click Login", "Form submits"));
        createStep(testCaseId, stepRequest(1, "Open login page", "Page renders"));
        var step3Id = createStep(testCaseId, stepRequest(3, "Verify dashboard", "Dashboard visible"));

        // List returns steps ordered by stepNumber ascending
        mockMvc.perform(get("/api/v1/test-cases/{tc}/steps", testCaseId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].stepNumber", is(1)))
                .andExpect(jsonPath("$[1].stepNumber", is(2)))
                .andExpect(jsonPath("$[2].stepNumber", is(3)))
                .andExpect(jsonPath("$[0].action", is("Open login page")));

        // Duplicate stepNumber rejected with 409
        mockMvc.perform(post("/api/v1/test-cases/{tc}/steps", testCaseId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stepRequest(1, "duplicate", "shouldn't happen"))))
                .andExpect(status().isConflict());

        // Get by id
        mockMvc.perform(get("/api/v1/test-cases/{tc}/steps/{s}", testCaseId, step3Id)
                        .param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stepNumber", is(3)));

        // Update — change actualResult + action (rich text with image reference)
        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("action", "## Verify dashboard\n\n![dash](https://example.com/dash.png)");
        updateBody.put("actualResult", "observed dashboard rendered");
        mockMvc.perform(put("/api/v1/test-cases/{tc}/steps/{s}", testCaseId, step3Id)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action", is("## Verify dashboard\n\n![dash](https://example.com/dash.png)")))
                .andExpect(jsonPath("$.actualResult", is("observed dashboard rendered")));

        // Clear actualResult via the dedicated flag
        Map<String, Object> clearBody = new LinkedHashMap<>();
        clearBody.put("clearActualResult", true);
        mockMvc.perform(put("/api/v1/test-cases/{tc}/steps/{s}", testCaseId, step3Id)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clearBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualResult", is(nullValue())));

        // Update step number — must remain unique per test case
        Map<String, Object> renumber = new LinkedHashMap<>();
        renumber.put("stepNumber", 1);
        mockMvc.perform(put("/api/v1/test-cases/{tc}/steps/{s}", testCaseId, step3Id)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renumber)))
                .andExpect(status().isConflict());

        // Delete one step
        mockMvc.perform(delete("/api/v1/test-cases/{tc}/steps/{s}", testCaseId, step3Id)
                        .param("project", "ground-control"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/test-cases/{tc}/steps", testCaseId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Deleting the parent test case cascade-deletes its remaining steps
        mockMvc.perform(delete("/api/v1/test-cases/{tc}", testCaseId).param("project", "ground-control"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/test-cases/{tc}", testCaseId).param("project", "ground-control"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crossTestCaseStepAccessRejected() throws Exception {
        var testCaseA = createTestCase("TC-STEP-A");
        var testCaseB = createTestCase("TC-STEP-B");
        var stepInAId = createStep(testCaseA, stepRequest(1, "act", "exp"));

        // Reading step A through test case B's path must 404 — service enforces
        // step belongs to the test case in the URL path.
        mockMvc.perform(get("/api/v1/test-cases/{tc}/steps/{s}", testCaseB, stepInAId)
                        .param("project", "ground-control"))
                .andExpect(status().isNotFound());
    }
}
