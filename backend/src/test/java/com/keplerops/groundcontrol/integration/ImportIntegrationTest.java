package com.keplerops.groundcontrol.integration;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class ImportIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String SDOC_SNIPPET =
            """
            [[SECTION]]
            TITLE: Wave 1 — Core

            [REQUIREMENT]
            UID: INT-REQ-001
            TITLE: First requirement
            STATEMENT: >>>
            First requirement statement.
            <<<
            COMMENT: GitHub issues: #1

            [REQUIREMENT]
            UID: INT-REQ-002
            TITLE: Second requirement
            STATEMENT: >>>
            Second requirement statement.
            <<<
            COMMENT: GitHub issues: #2
            RELATIONS:
            - TYPE: Parent
              VALUE: INT-REQ-001

            [[/SECTION]]
            """;

    @Test
    void importSdocSnippet_createsRequirementsAndRelations() throws Exception {
        var file =
                new MockMultipartFile("file", "test.sdoc", "text/plain", SDOC_SNIPPET.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/admin/import/strictdoc").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importId", notNullValue()))
                .andExpect(jsonPath("$.requirementsParsed", is(2)))
                .andExpect(jsonPath("$.requirementsCreated", is(2)))
                .andExpect(jsonPath("$.requirementsUpdated", is(0)))
                .andExpect(jsonPath("$.relationsCreated", is(1)))
                .andExpect(jsonPath("$.traceabilityLinksCreated", is(2)));
    }

    @Test
    void importSdocTwice_isIdempotent() throws Exception {
        var file1 =
                new MockMultipartFile("file", "test.sdoc", "text/plain", SDOC_SNIPPET.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/admin/import/strictdoc").file(file1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirementsCreated", is(2)));

        var file2 =
                new MockMultipartFile("file", "test.sdoc", "text/plain", SDOC_SNIPPET.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/admin/import/strictdoc").file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirementsCreated", is(0)))
                .andExpect(jsonPath("$.requirementsUpdated", is(2)))
                .andExpect(jsonPath("$.relationsSkipped", is(1)))
                .andExpect(jsonPath("$.traceabilityLinksSkipped", is(2)));
    }
}
