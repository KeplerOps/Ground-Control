package com.keplerops.groundcontrol.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class RequirementControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> validRequest(String uid) {
        return Map.of(
                "uid",
                uid,
                "title",
                "Title for " + uid,
                "statement",
                "Statement for " + uid,
                "requirementType",
                "FUNCTIONAL",
                "priority",
                "MUST");
    }

    private String createAndReturnId(String uid) throws Exception {
        var result = mockMvc.perform(post("/api/v1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(uid))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    @Test
    void createRequirement_returns201WithDraftStatus() throws Exception {
        mockMvc.perform(post("/api/v1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("REQ-C-001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.uid", is("REQ-C-001")));
    }

    @Test
    void getById_returns200() throws Exception {
        var id = createAndReturnId("REQ-C-002");

        mockMvc.perform(get("/api/v1/requirements/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("REQ-C-002")));
    }

    @Test
    void getByUid_returns200() throws Exception {
        createAndReturnId("REQ-C-003");

        mockMvc.perform(get("/api/v1/requirements/uid/REQ-C-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid", is("REQ-C-003")));
    }

    @Test
    void listRequirements_returns200WithPagination() throws Exception {
        createAndReturnId("REQ-C-004");

        mockMvc.perform(get("/api/v1/requirements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void updateRequirement_returns200() throws Exception {
        var id = createAndReturnId("REQ-C-005");

        var updateBody = Map.of(
                "uid", "REQ-C-005",
                "title", "Updated Title",
                "statement", "Updated Statement");

        mockMvc.perform(put("/api/v1/requirements/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")));
    }

    @Test
    void transitionStatus_returns200() throws Exception {
        var id = createAndReturnId("REQ-C-006");

        mockMvc.perform(post("/api/v1/requirements/" + id + "/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void archiveRequirement_returns200() throws Exception {
        var id = createAndReturnId("REQ-C-007");

        // Must be ACTIVE first
        mockMvc.perform(post("/api/v1/requirements/" + id + "/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/requirements/" + id + "/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARCHIVED")))
                .andExpect(jsonPath("$.archivedAt", notNullValue()));
    }

    @Test
    void createRelation_returns201() throws Exception {
        var sourceId = createAndReturnId("REQ-C-008");
        var targetId = createAndReturnId("REQ-C-009");

        var body = Map.of("targetId", targetId, "relationType", "DEPENDS_ON");

        mockMvc.perform(post("/api/v1/requirements/" + sourceId + "/relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationType", is("DEPENDS_ON")));
    }

    @Test
    void getRelations_returns200() throws Exception {
        var id = createAndReturnId("REQ-C-010");

        mockMvc.perform(get("/api/v1/requirements/" + id + "/relations")).andExpect(status().isOk());
    }

    @Test
    void getNonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/requirements/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("not_found")));
    }

    @Test
    void invalidTransition_returns422() throws Exception {
        var id = createAndReturnId("REQ-C-011");

        mockMvc.perform(post("/api/v1/requirements/" + id + "/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ARCHIVED\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("invalid_status_transition")));
    }

    @Test
    void blankTitle_returns422() throws Exception {
        var body = Map.of("uid", "REQ-C-012", "title", "", "statement", "Statement");

        mockMvc.perform(post("/api/v1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code", is("validation_error")));
    }

    @Test
    void duplicateUid_returns409() throws Exception {
        createAndReturnId("REQ-C-013");

        mockMvc.perform(post("/api/v1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("REQ-C-013"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("conflict")));
    }

    @Test
    void filteredList_byPriority_returnsFiltered() throws Exception {
        // MUST priority (default from validRequest)
        createAndReturnId("REQ-C-030");

        // SHOULD priority
        var shouldReq = new HashMap<>(validRequest("REQ-C-031"));
        shouldReq.put("priority", "SHOULD");
        mockMvc.perform(post("/api/v1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shouldReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/requirements").param("priority", "SHOULD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].uid", is("REQ-C-031")));
    }

    @Test
    void filteredList_byStatus_returnsFiltered() throws Exception {
        createAndReturnId("REQ-C-014");
        var activeId = createAndReturnId("REQ-C-015");
        mockMvc.perform(post("/api/v1/requirements/" + activeId + "/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/requirements").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].uid", is("REQ-C-015")));
    }

    @Test
    void filteredList_bySearch_returnsFiltered() throws Exception {
        createAndReturnId("REQ-C-016");

        mockMvc.perform(get("/api/v1/requirements").param("search", "REQ-C-016"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].uid", is("REQ-C-016")));
    }

    @Test
    void deleteRelation_returns204() throws Exception {
        var sourceId = createAndReturnId("REQ-C-017");
        var targetId = createAndReturnId("REQ-C-018");

        var body = Map.of("targetId", targetId, "relationType", "DEPENDS_ON");
        var result = mockMvc.perform(post("/api/v1/requirements/" + sourceId + "/relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        var relationId = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(delete("/api/v1/requirements/" + sourceId + "/relations/" + relationId))
                .andExpect(status().isNoContent());
    }
}
