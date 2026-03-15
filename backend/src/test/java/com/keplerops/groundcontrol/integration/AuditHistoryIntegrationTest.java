package com.keplerops.groundcontrol.integration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditHistoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    private UUID requirementId;

    @AfterAll
    void cleanup() throws Exception {
        try (var conn = dataSource.getConnection();
                var stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "DELETE FROM requirement_audit WHERE id IN (SELECT id FROM requirement WHERE uid LIKE 'AUDIT-%')");
            stmt.executeUpdate("DELETE FROM requirement WHERE uid LIKE 'AUDIT-%'");
        }
    }

    @Test
    @Order(1)
    void createAndUpdateThenGetHistory() throws Exception {
        // Create
        var createBody = Map.of(
                "uid", "AUDIT-001",
                "title", "Audit test requirement",
                "statement", "Created for audit history test",
                "requirementType", "FUNCTIONAL",
                "priority", "MUST");
        var createResult = mockMvc.perform(post("/api/v1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn();
        requirementId = UUID.fromString(objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText());

        // Update title
        var updateBody = Map.of(
                "uid", "AUDIT-001",
                "title", "Updated audit title",
                "statement", "Created for audit history test");
        mockMvc.perform(put("/api/v1/requirements/" + requirementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk());

        // Get history — expect 2 revisions: INSERT and UPDATE
        mockMvc.perform(get("/api/v1/requirements/" + requirementId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].revisionType", is("ADD")))
                .andExpect(jsonPath("$[0].timestamp", notNullValue()))
                .andExpect(jsonPath("$[0].snapshot.uid", is("AUDIT-001")))
                .andExpect(jsonPath("$[0].snapshot.title", is("Audit test requirement")))
                .andExpect(jsonPath("$[0].actor", nullValue()))
                .andExpect(jsonPath("$[1].revisionType", is("MOD")))
                .andExpect(jsonPath("$[1].snapshot.title", is("Updated audit title")));
    }

    @Test
    @Order(2)
    void historyForNonexistentRequirement_returns404() throws Exception {
        var fakeId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/requirements/" + fakeId + "/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("not_found")));
    }
}
