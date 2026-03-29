package com.keplerops.groundcontrol.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class ComplianceIntegrationTest extends BaseIntegrationTest {

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
            stmt.executeUpdate("DELETE FROM traceability_link_audit WHERE id IN "
                    + "(SELECT id FROM traceability_link WHERE requirement_id IN "
                    + "(SELECT id FROM requirement WHERE uid LIKE 'COMPL-%'))");
            stmt.executeUpdate("DELETE FROM traceability_link WHERE requirement_id IN "
                    + "(SELECT id FROM requirement WHERE uid LIKE 'COMPL-%')");
            stmt.executeUpdate("DELETE FROM requirement_relation_audit WHERE id IN "
                    + "(SELECT id FROM requirement_relation WHERE source_id IN "
                    + "(SELECT id FROM requirement WHERE uid LIKE 'COMPL-%'))");
            stmt.executeUpdate("DELETE FROM requirement_relation WHERE source_id IN "
                    + "(SELECT id FROM requirement WHERE uid LIKE 'COMPL-%')");
            stmt.executeUpdate(
                    "DELETE FROM requirement_audit WHERE id IN (SELECT id FROM requirement WHERE uid LIKE 'COMPL-%')");
            stmt.executeUpdate("DELETE FROM requirement WHERE uid LIKE 'COMPL-%'");
        }
    }

    @Test
    @Order(1)
    void transitionWithReason_reasonAppearsInHistory() throws Exception {
        // Create requirement
        var createBody = Map.of(
                "uid", "COMPL-001",
                "title", "Compliance test",
                "statement", "Testing transition reasons",
                "requirementType", "FUNCTIONAL",
                "priority", "MUST");
        var createResult = mockMvc.perform(post("/api/v1/requirements")
                        .header("X-Actor", "compliance-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn();
        requirementId = UUID.fromString(objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asText());

        // Transition with reason
        var transitionBody = Map.of("status", "ACTIVE", "reason", "Approved by review board");
        mockMvc.perform(post("/api/v1/requirements/" + requirementId + "/transition")
                        .header("X-Actor", "compliance-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionBody)))
                .andExpect(status().isOk());

        // Verify reason in history
        mockMvc.perform(get("/api/v1/requirements/" + requirementId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].reason", is("Approved by review board")));
    }

    @Test
    @Order(2)
    void transitionWithoutReason_reasonIsNull() throws Exception {
        // History from step 1 — the ADD revision should have null reason
        mockMvc.perform(get("/api/v1/requirements/" + requirementId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").doesNotExist());
    }

    @Test
    @Order(3)
    void timeline_reasonAppearsInEntries() throws Exception {
        mockMvc.perform(get("/api/v1/requirements/" + requirementId + "/timeline")
                        .param("changeCategory", "REQUIREMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason", is("Approved by review board")));
    }

    @Test
    @Order(4)
    void timeline_filterByActor_returnsOnlyMatchingEntries() throws Exception {
        mockMvc.perform(get("/api/v1/requirements/" + requirementId + "/timeline")
                        .param("actor", "compliance-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));

        // Non-existent actor should return empty
        mockMvc.perform(get("/api/v1/requirements/" + requirementId + "/timeline")
                        .param("actor", "nonexistent-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Order(5)
    void projectTimeline_returnsEntriesAcrossRequirements() throws Exception {
        // Create a second requirement
        var createBody = Map.of(
                "uid", "COMPL-002",
                "title", "Second compliance test",
                "statement", "Another requirement",
                "requirementType", "NON_FUNCTIONAL",
                "priority", "SHOULD");
        mockMvc.perform(post("/api/v1/requirements")
                        .header("X-Actor", "other-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated());

        // Project timeline should include entries from both requirements
        mockMvc.perform(get("/api/v1/audit/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    @Order(6)
    void projectTimeline_filterByActor() throws Exception {
        mockMvc.perform(get("/api/v1/audit/timeline").param("actor", "other-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].actor", is("other-user")));
    }

    @Test
    @Order(7)
    void exportTimeline_returnsCsv() throws Exception {
        mockMvc.perform(get("/api/v1/audit/timeline/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("audit-timeline.csv")))
                .andExpect(content().string(containsString("timestamp,actor,reason,change_category")))
                .andExpect(content().string(containsString("compliance-user")));
    }
}
