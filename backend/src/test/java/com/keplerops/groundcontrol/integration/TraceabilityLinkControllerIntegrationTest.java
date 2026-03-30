package com.keplerops.groundcontrol.integration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class TraceabilityLinkControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createRequirementAndReturnId(String uid) throws Exception {
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
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    private String createLinkAndReturnId(String requirementId) throws Exception {
        var body = Map.of(
                "artifactType", "GITHUB_ISSUE",
                "artifactIdentifier", "GH-42",
                "artifactUrl", "https://github.com/org/repo/issues/42",
                "artifactTitle", "Fix the bug",
                "linkType", "IMPLEMENTS");
        var result = mockMvc.perform(post("/api/v1/requirements/" + requirementId + "/traceability")
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
    void createLink_returns201() throws Exception {
        var reqId = createRequirementAndReturnId("REQ-TL-001");

        var body = Map.of(
                "artifactType", "GITHUB_ISSUE",
                "artifactIdentifier", "GH-42",
                "artifactUrl", "https://github.com/org/repo/issues/42",
                "artifactTitle", "Fix the bug",
                "linkType", "IMPLEMENTS");

        mockMvc.perform(post("/api/v1/requirements/" + reqId + "/traceability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.artifactType", is("GITHUB_ISSUE")))
                .andExpect(jsonPath("$.artifactIdentifier", is("GH-42")))
                .andExpect(jsonPath("$.linkType", is("IMPLEMENTS")))
                .andExpect(jsonPath("$.syncStatus", is("SYNCED")));
    }

    @ParameterizedTest
    @EnumSource(ArtifactType.class)
    void createLink_allArtifactTypes_returns201(ArtifactType type) throws Exception {
        var reqId = createRequirementAndReturnId("REQ-AT-" + type.name());

        var body = Map.of(
                "artifactType", type.name(), "artifactIdentifier", "id:" + type.name(), "linkType", "IMPLEMENTS");

        mockMvc.perform(post("/api/v1/requirements/" + reqId + "/traceability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.artifactType", is(type.name())))
                .andExpect(jsonPath("$.artifactIdentifier", is("id:" + type.name())));
    }

    @Test
    void getLinks_returns200() throws Exception {
        var reqId = createRequirementAndReturnId("REQ-TL-002");
        createLinkAndReturnId(reqId);

        mockMvc.perform(get("/api/v1/requirements/" + reqId + "/traceability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].artifactIdentifier", is("GH-42")));
    }

    @Test
    void deleteLink_returns204() throws Exception {
        var reqId = createRequirementAndReturnId("REQ-TL-003");
        var linkId = createLinkAndReturnId(reqId);

        mockMvc.perform(delete("/api/v1/requirements/" + reqId + "/traceability/" + linkId))
                .andExpect(status().isNoContent());
    }

    @Test
    void createLink_requirementNotFound_returns404() throws Exception {
        var body = Map.of(
                "artifactType", "GITHUB_ISSUE",
                "artifactIdentifier", "GH-42",
                "linkType", "IMPLEMENTS");

        mockMvc.perform(post("/api/v1/requirements/" + UUID.randomUUID() + "/traceability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("not_found")));
    }
}
