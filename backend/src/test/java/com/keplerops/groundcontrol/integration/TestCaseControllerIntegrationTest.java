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

    @Test
    void move_copy_reorder_roundTrip() throws Exception {
        // Create a folder to place the test case into.
        Map<String, Object> folderReq = new LinkedHashMap<>();
        folderReq.put("title", "Hierarchy bucket");
        var folderResult = mockMvc.perform(post("/api/v1/test-cases/folders")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(folderReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String folderId = objectMapper
                .readTree(folderResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        // Create source test case at the root.
        var srcResult = mockMvc.perform(post("/api/v1/test-cases")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("TC-MV-001"))))
                .andExpect(status().isCreated())
                .andReturn();
        String srcId = objectMapper
                .readTree(srcResult.getResponse().getContentAsString())
                .get("id")
                .asText();

        // Move into folder.
        Map<String, Object> moveBody = new LinkedHashMap<>();
        moveBody.put("parentFolderId", folderId);
        mockMvc.perform(put("/api/v1/test-cases/{id}/move", srcId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId", is(folderId)));

        // Copy into the same folder under a new UID. Per the move/copy
        // parity convention, callers pass the desired parentFolderId
        // explicitly; null or omitted means project root.
        Map<String, Object> copyBody = new LinkedHashMap<>();
        copyBody.put("newUid", "TC-MV-001-COPY");
        copyBody.put("parentFolderId", folderId);
        mockMvc.perform(post("/api/v1/test-cases/{id}/copy", srcId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(copyBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid", is("TC-MV-001-COPY")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.parentFolderId", is(folderId)));

        // Copy to root by passing parentFolderId = null explicitly.
        Map<String, Object> copyToRootBody = new LinkedHashMap<>();
        copyToRootBody.put("newUid", "TC-MV-001-ROOT");
        copyToRootBody.put("parentFolderId", null);
        mockMvc.perform(post("/api/v1/test-cases/{id}/copy", srcId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(copyToRootBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentFolderId", is(nullValue())));

        // Copy with the existing source UID is rejected.
        Map<String, Object> dupBody = new LinkedHashMap<>();
        dupBody.put("newUid", "TC-MV-001");
        mockMvc.perform(post("/api/v1/test-cases/{id}/copy", srcId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupBody)))
                .andExpect(status().isConflict());

        // Move back to root — assert parentFolderId actually nulled out
        // (test-quality cycle 2): a regression where 200 OK comes back
        // but the test case stays in its folder is otherwise invisible
        // because this is the last step.
        Map<String, Object> rootMove = new LinkedHashMap<>();
        rootMove.put("parentFolderId", null);
        mockMvc.perform(put("/api/v1/test-cases/{id}/move", srcId)
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rootMove)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId", is(nullValue())));
    }
}
