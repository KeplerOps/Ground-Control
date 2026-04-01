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
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdrControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> validRequest() {
        return Map.of(
                "uid", "ADR-001",
                "title", "Use PostgreSQL",
                "decisionDate", "2026-03-31",
                "context", "We need a database",
                "decision", "Use PostgreSQL for relational data",
                "consequences", "Proven, good ecosystem");
    }

    private String createAndReturnId(Map<String, Object> request) throws Exception {
        var result = mockMvc.perform(post("/api/v1/adrs")
                        .param("project", "ground-control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    @Nested
    class Create {

        @Test
        void createAdr_returns201() throws Exception {
            mockMvc.perform(post("/api/v1/adrs")
                            .param("project", "ground-control")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uid", is("ADR-001")))
                    .andExpect(jsonPath("$.title", is("Use PostgreSQL")))
                    .andExpect(jsonPath("$.status", is("PROPOSED")))
                    .andExpect(jsonPath("$.decisionDate", is("2026-03-31")))
                    .andExpect(jsonPath("$.context", is("We need a database")))
                    .andExpect(jsonPath("$.decision", is("Use PostgreSQL for relational data")))
                    .andExpect(jsonPath("$.consequences", is("Proven, good ecosystem")))
                    .andExpect(jsonPath("$.projectIdentifier", is("ground-control")));
        }

        @Test
        void createDuplicateUid_returns409() throws Exception {
            createAndReturnId(validRequest());

            mockMvc.perform(post("/api/v1/adrs")
                            .param("project", "ground-control")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isConflict());
        }

        @Test
        void createWithMissingUid_returns422() throws Exception {
            var request = Map.of("title", "Test", "decisionDate", "2026-03-31");

            mockMvc.perform(post("/api/v1/adrs")
                            .param("project", "ground-control")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    class Read {

        @Test
        void getById_returnsAdr() throws Exception {
            var id = createAndReturnId(validRequest());

            mockMvc.perform(get("/api/v1/adrs/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid", is("ADR-001")));
        }

        @Test
        void getByUid_returnsAdr() throws Exception {
            createAndReturnId(validRequest());

            mockMvc.perform(get("/api/v1/adrs/uid/{uid}", "ADR-001").param("project", "ground-control"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uid", is("ADR-001")));
        }

        @Test
        void getNonExistent_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/adrs/{id}", "00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void listByProject_returnsAll() throws Exception {
            createAndReturnId(validRequest());

            Map<String, Object> request2 = Map.of(
                    "uid", "ADR-002",
                    "title", "Use Spring Boot",
                    "decisionDate", "2026-03-31");
            createAndReturnId(request2);

            mockMvc.perform(get("/api/v1/adrs").param("project", "ground-control"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    class Update {

        @Test
        void updateTitle_returnsUpdated() throws Exception {
            var id = createAndReturnId(validRequest());

            mockMvc.perform(put("/api/v1/adrs/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", "Use MySQL"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Use MySQL")))
                    .andExpect(jsonPath("$.uid", is("ADR-001")));
        }

        @Test
        void partialUpdate_preservesOtherFields() throws Exception {
            var id = createAndReturnId(validRequest());

            mockMvc.perform(put("/api/v1/adrs/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", "Updated"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.context", is("We need a database")))
                    .andExpect(jsonPath("$.decision", is("Use PostgreSQL for relational data")));
        }
    }

    @Nested
    class Delete {

        @Test
        void deleteAdr_returns204() throws Exception {
            var id = createAndReturnId(validRequest());

            mockMvc.perform(delete("/api/v1/adrs/{id}", id)).andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/adrs/{id}", id)).andExpect(status().isNotFound());
        }
    }

    @Nested
    class StatusTransitions {

        @Test
        void proposedToAccepted_succeeds() throws Exception {
            var id = createAndReturnId(validRequest());

            mockMvc.perform(put("/api/v1/adrs/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("status", "ACCEPTED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ACCEPTED")));
        }

        @Test
        void proposedToDeprecated_fails() throws Exception {
            var id = createAndReturnId(validRequest());

            mockMvc.perform(put("/api/v1/adrs/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("status", "DEPRECATED"))))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void fullLifecycle_proposedToAcceptedToSuperseded() throws Exception {
            var id = createAndReturnId(validRequest());

            mockMvc.perform(put("/api/v1/adrs/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("status", "ACCEPTED"))))
                    .andExpect(status().isOk());

            mockMvc.perform(put("/api/v1/adrs/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("status", "SUPERSEDED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("SUPERSEDED")));
        }
    }

    @Nested
    class ReverseTraceability {

        @Test
        void noLinkedRequirements_returnsEmpty() throws Exception {
            var id = createAndReturnId(validRequest());

            mockMvc.perform(get("/api/v1/adrs/{id}/requirements", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
