package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AssetRelationLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    private UUID relationId;

    @AfterAll
    void cleanup() throws Exception {
        try (var conn = dataSource.getConnection();
                var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM asset_relation_audit WHERE id IN "
                    + "(SELECT ar.id FROM asset_relation ar "
                    + "JOIN operational_asset src ON src.id = ar.source_id "
                    + "WHERE src.uid LIKE 'ARL-%')");
            stmt.executeUpdate("DELETE FROM asset_relation WHERE source_id IN "
                    + "(SELECT id FROM operational_asset WHERE uid LIKE 'ARL-%') "
                    + "OR target_id IN (SELECT id FROM operational_asset WHERE uid LIKE 'ARL-%')");
            stmt.executeUpdate("DELETE FROM operational_asset_audit WHERE id IN "
                    + "(SELECT id FROM operational_asset WHERE uid LIKE 'ARL-%')");
            stmt.executeUpdate("DELETE FROM operational_asset WHERE uid LIKE 'ARL-%'");
        }
    }

    @Test
    void assetRelationTracksLifecycleMetadataAndAuditHistory() throws Exception {
        UUID sourceId = createAsset("ARL-SRC", "Lifecycle Source");
        UUID targetId = createAsset("ARL-TGT", "Lifecycle Target");

        Instant collectedAt = Instant.parse("2026-04-01T12:00:00Z");
        JsonNode createJson = objectMapper.readTree(mockMvc.perform(post("/api/v1/assets/{id}/relations", sourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetId", targetId.toString(),
                                "relationType", "DEPENDS_ON",
                                "description", "Observed dependency",
                                "sourceSystem", "AWS_CONFIG",
                                "externalSourceId", "cfg-123",
                                "collectedAt", collectedAt.toString(),
                                "confidence", "0.80"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Observed dependency"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString());

        relationId = UUID.fromString(createJson.get("id").asText());
        Instant createdAt = Instant.parse(createJson.get("createdAt").asText());
        Instant initialUpdatedAt = Instant.parse(createJson.get("updatedAt").asText());
        assertThat(initialUpdatedAt).isEqualTo(createdAt);

        Thread.sleep(10L);

        JsonNode updateJson = objectMapper.readTree(mockMvc.perform(put(
                                "/api/v1/assets/{id}/relations/{relationId}", sourceId, relationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Refined dependency",
                                "sourceSystem", "CMDB",
                                "externalSourceId", "cmdb-789",
                                "collectedAt",
                                        Instant.parse("2026-04-02T12:00:00Z").toString(),
                                "confidence", "0.95"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Refined dependency"))
                .andExpect(jsonPath("$.sourceSystem").value("CMDB"))
                .andExpect(jsonPath("$.confidence").value("0.95"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString());

        Instant updatedAt = Instant.parse(updateJson.get("updatedAt").asText());
        assertThat(updatedAt).isAfter(initialUpdatedAt);

        try (var conn = dataSource.getConnection()) {
            try (var stmt = conn.prepareStatement("SELECT created_at, updated_at FROM asset_relation WHERE id = ?")) {
                stmt.setObject(1, relationId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getObject("created_at", Instant.class)).isNotNull();
                    assertThat(rs.getObject("updated_at", Instant.class)).isNotNull();
                    assertThat(rs.getObject("updated_at", Instant.class))
                            .isAfterOrEqualTo(rs.getObject("created_at", Instant.class));
                }
            }

            try (var stmt = conn.prepareStatement("SELECT COUNT(*) FROM asset_relation_audit WHERE id = ?")) {
                stmt.setObject(1, relationId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
                }
            }

            try (var stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM asset_relation_audit WHERE id = ? AND updated_at IS NOT NULL")) {
                stmt.setObject(1, relationId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
                }
            }
        }
    }

    private UUID createAsset(String uid, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/assets")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "uid",
                                uid,
                                "name",
                                name,
                                "description",
                                "Integration test asset",
                                "assetType",
                                "SERVICE"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }
}
