package com.keplerops.groundcontrol.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the parent-child ownership boundary on the nested requirement relation/traceability
 * routes end-to-end (issue #432): a child resource that belongs to a different requirement must be
 * <em>indistinguishable</em> from a missing child — the same 404 and the same {@code ErrorResponse}
 * envelope (down to the {@code error.message}, with no {@code error.detail} block and no mention of
 * the child's real parent) — and the targeted child must be left intact. Each test issues both the
 * ownership-mismatch request and a genuinely-missing-child request and asserts the two envelopes
 * have the same shape.
 *
 * <p>The ownership checks live in {@code RequirementService.deleteRelation},
 * {@code TraceabilityService.deleteLink}, and
 * {@code AuditService.getRelationHistory}/{@code getTraceabilityLinkHistory}; this test pins their
 * HTTP contract. The check runs before any Hibernate Envers read, so a {@code @Transactional}
 * rollback is sufficient here.
 */
@AutoConfigureMockMvc
@Transactional
class NestedRouteOwnershipIntegrationTest extends BaseIntegrationTest {

    private static final String RELATION_NOT_FOUND = "Relation not found: ";
    private static final String LINK_NOT_FOUND = "Traceability link not found: ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createRequirement(String uid) throws Exception {
        var body = Map.of(
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
        var result = mockMvc.perform(post("/api/v1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private String createRelation(String sourceId, String targetId) throws Exception {
        var body = Map.of("targetId", targetId, "relationType", "DEPENDS_ON");
        var result = mockMvc.perform(post("/api/v1/requirements/" + sourceId + "/relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private String createTraceabilityLink(String requirementId) throws Exception {
        // DOCUMENTS links have no ACTIVE-status precondition, unlike IMPLEMENTS.
        var body = Map.of(
                "artifactType", "CODE_FILE",
                "artifactIdentifier", "src/main/java/Owned.java",
                "linkType", "DOCUMENTS");
        var result = mockMvc.perform(post("/api/v1/requirements/" + requirementId + "/traceability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private String idOf(MvcResult result) throws Exception {
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    private JsonNode errorBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("error");
    }

    /**
     * Asserts the response carries exactly the {@code ErrorResponse} a genuinely-missing child of
     * the same id would produce: HTTP 404, {@code error.code = "not_found"}, {@code error.message}
     * equal to {@code "<typePrefix><childId>"}, and no {@code error.detail} block — so nothing about
     * the child's real parent leaks.
     */
    private void assertMissingChildEnvelope(MvcResult result, String typePrefix, String childId) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        var error = errorBody(result);
        assertThat(error.path("code").asText()).isEqualTo("not_found");
        assertThat(error.path("message").asText()).isEqualTo(typePrefix + childId);
        assertThat(error.has("detail"))
                .as("not_found envelope must not include a detail block")
                .isFalse();
    }

    @Test
    void relationHistory_whenRequirementDoesNotOwnRelation_isIndistinguishableFromMissingRelation() throws Exception {
        var sourceId = createRequirement("NRO-RH-SRC");
        var targetId = createRequirement("NRO-RH-TGT");
        var outsiderId = createRequirement("NRO-RH-OUT");
        var relationId = createRelation(sourceId, targetId);
        var ghostRelationId = UUID.randomUUID().toString();

        var mismatch = mockMvc.perform(
                        get("/api/v1/requirements/" + outsiderId + "/relations/" + relationId + "/history"))
                .andExpect(status().isNotFound())
                .andReturn();
        var missing = mockMvc.perform(
                        get("/api/v1/requirements/" + outsiderId + "/relations/" + ghostRelationId + "/history"))
                .andExpect(status().isNotFound())
                .andReturn();

        assertMissingChildEnvelope(mismatch, RELATION_NOT_FOUND, relationId);
        assertMissingChildEnvelope(missing, RELATION_NOT_FOUND, ghostRelationId);
    }

    @Test
    void deleteRelation_whenRequirementDoesNotOwnRelation_isIndistinguishableFromMissing_andRelationSurvives()
            throws Exception {
        var sourceId = createRequirement("NRO-RD-SRC");
        var targetId = createRequirement("NRO-RD-TGT");
        var outsiderId = createRequirement("NRO-RD-OUT");
        var relationId = createRelation(sourceId, targetId);
        var ghostRelationId = UUID.randomUUID().toString();

        var mismatch = mockMvc.perform(delete("/api/v1/requirements/" + outsiderId + "/relations/" + relationId))
                .andExpect(status().isNotFound())
                .andReturn();
        var missing = mockMvc.perform(delete("/api/v1/requirements/" + outsiderId + "/relations/" + ghostRelationId))
                .andExpect(status().isNotFound())
                .andReturn();

        assertMissingChildEnvelope(mismatch, RELATION_NOT_FOUND, relationId);
        assertMissingChildEnvelope(missing, RELATION_NOT_FOUND, ghostRelationId);

        // The rejected delete must not have removed the relation from its real source requirement.
        mockMvc.perform(get("/api/v1/requirements/" + sourceId + "/relations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(relationId)));
    }

    @Test
    void traceabilityLinkHistory_whenRequirementDoesNotOwnLink_isIndistinguishableFromMissingLink() throws Exception {
        var ownerId = createRequirement("NRO-TH-OWN");
        var outsiderId = createRequirement("NRO-TH-OUT");
        var linkId = createTraceabilityLink(ownerId);
        var ghostLinkId = UUID.randomUUID().toString();

        var mismatch = mockMvc.perform(
                        get("/api/v1/requirements/" + outsiderId + "/traceability/" + linkId + "/history"))
                .andExpect(status().isNotFound())
                .andReturn();
        var missing = mockMvc.perform(
                        get("/api/v1/requirements/" + outsiderId + "/traceability/" + ghostLinkId + "/history"))
                .andExpect(status().isNotFound())
                .andReturn();

        assertMissingChildEnvelope(mismatch, LINK_NOT_FOUND, linkId);
        assertMissingChildEnvelope(missing, LINK_NOT_FOUND, ghostLinkId);
    }

    @Test
    void deleteTraceabilityLink_whenRequirementDoesNotOwnLink_isIndistinguishableFromMissing_andLinkSurvives()
            throws Exception {
        var ownerId = createRequirement("NRO-TD-OWN");
        var outsiderId = createRequirement("NRO-TD-OUT");
        var linkId = createTraceabilityLink(ownerId);
        var ghostLinkId = UUID.randomUUID().toString();

        var mismatch = mockMvc.perform(delete("/api/v1/requirements/" + outsiderId + "/traceability/" + linkId))
                .andExpect(status().isNotFound())
                .andReturn();
        var missing = mockMvc.perform(delete("/api/v1/requirements/" + outsiderId + "/traceability/" + ghostLinkId))
                .andExpect(status().isNotFound())
                .andReturn();

        assertMissingChildEnvelope(mismatch, LINK_NOT_FOUND, linkId);
        assertMissingChildEnvelope(missing, LINK_NOT_FOUND, ghostLinkId);

        // The rejected delete must not have removed the link from its real owner requirement.
        mockMvc.perform(get("/api/v1/requirements/" + ownerId + "/traceability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(linkId)));
    }
}
