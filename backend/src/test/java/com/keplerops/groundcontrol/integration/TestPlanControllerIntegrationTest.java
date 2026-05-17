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
class TestPlanControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Map<String, Object> validRequest(String uid) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("uid", uid);
        request.put("name", "Wave-1 acceptance");
        request.put("description", "scope notes for the wave-1 acceptance run");
        request.put("product", "ground-control");
        request.put("version", "1.2.0");
        request.put("build", "build-42");
        request.put("startDate", "2026-06-01");
        request.put("endDate", "2026-06-30");
        return request;
    }

    @Test
    void create_get_list_update_transition_delete_roundTrip() throws Exception {
        var createResult = mockMvc.perform(post("/api/v1/test-plans")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("TP-INT-001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid", is("TP-INT-001")))
                .andExpect(jsonPath("$.name", is("Wave-1 acceptance")))
                .andExpect(jsonPath("$.product", is("ground-control")))
                .andExpect(jsonPath("$.version", is("1.2.0")))
                .andExpect(jsonPath("$.build", is("build-42")))
                .andExpect(jsonPath("$.startDate", is("2026-06-01")))
                .andExpect(jsonPath("$.endDate", is("2026-06-30")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andReturn();
        String planId = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        // Duplicate UID rejected
        mockMvc.perform(post("/api/v1/test-plans")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("TP-INT-001"))))
                .andExpect(status().isConflict());

        // Schedule inversion rejected on create
        Map<String, Object> inverted = validRequest("TP-INT-002");
        inverted.put("startDate", "2026-07-30");
        inverted.put("endDate", "2026-07-01");
        mockMvc.perform(post("/api/v1/test-plans")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inverted)))
                .andExpect(status().isUnprocessableEntity());

        // Get by id
        mockMvc.perform(get("/api/v1/test-plans/{id}", planId).param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("TP-INT-001")));

        // Get by uid
        mockMvc.perform(get("/api/v1/test-plans/uid/{uid}", "TP-INT-001").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(planId)));

        // List
        mockMvc.perform(get("/api/v1/test-plans").param("project", "ground-control"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uid", is("TP-INT-001")));

        // Update — name + clear build
        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("name", "Wave-1 acceptance (renamed)");
        updateBody.put("clearBuild", true);
        mockMvc.perform(put("/api/v1/test-plans/{id}", planId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Wave-1 acceptance (renamed)")))
                .andExpect(jsonPath("$.build").doesNotExist());

        // Transition DRAFT -> ACTIVE
        mockMvc.perform(put("/api/v1/test-plans/{id}/status", planId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // Illegal transition ACTIVE -> DRAFT — domain validation rejection
        mockMvc.perform(put("/api/v1/test-plans/{id}/status", planId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isUnprocessableEntity());

        // Transition ACTIVE -> IN_PROGRESS -> COMPLETED
        mockMvc.perform(put("/api/v1/test-plans/{id}/status", planId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
        mockMvc.perform(put("/api/v1/test-plans/{id}/status", planId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        // Delete
        mockMvc.perform(delete("/api/v1/test-plans/{id}", planId).param("project", "ground-control"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/test-plans/{id}", planId).param("project", "ground-control"))
                .andExpect(status().isNotFound());
    }
}
