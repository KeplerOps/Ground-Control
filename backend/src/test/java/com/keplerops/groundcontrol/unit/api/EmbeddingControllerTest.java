package com.keplerops.groundcontrol.unit.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.keplerops.groundcontrol.api.admin.EmbeddingController;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.BatchEmbeddingResult;
import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingResult;
import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingService;
import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmbeddingController.class)
class EmbeddingControllerTest {

    private static final UUID REQUIREMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmbeddingService embeddingService;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void embedReturnsResult() throws Exception {
        when(embeddingService.embedRequirement(REQUIREMENT_ID))
                .thenReturn(new EmbeddingResult(REQUIREMENT_ID, "embedded", "text-embedding-3-large", "abc123"));

        mockMvc.perform(post("/api/v1/embeddings/{requirementId}", REQUIREMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirementId", is(REQUIREMENT_ID.toString())))
                .andExpect(jsonPath("$.status", is("embedded")))
                .andExpect(jsonPath("$.modelId", is("text-embedding-3-large")))
                .andExpect(jsonPath("$.contentHash", is("abc123")));
    }

    @Test
    void statusReturnsEmbeddingStatus() throws Exception {
        when(embeddingService.getEmbeddingStatus(REQUIREMENT_ID))
                .thenReturn(new EmbeddingStatus(
                        REQUIREMENT_ID,
                        true,
                        false,
                        false,
                        "text-embedding-3-large",
                        "text-embedding-3-large",
                        Instant.parse("2026-04-05T12:00:00Z")));

        mockMvc.perform(get("/api/v1/embeddings/{requirementId}/status", REQUIREMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirementId", is(REQUIREMENT_ID.toString())))
                .andExpect(jsonPath("$.hasEmbedding", is(true)))
                .andExpect(jsonPath("$.isStale", is(false)))
                .andExpect(jsonPath("$.modelMismatch", is(false)));
    }

    @Test
    void batchEmbedResolvesProjectAndReturnsSummary() throws Exception {
        when(projectService.resolveProjectId("ground-control")).thenReturn(PROJECT_ID);
        when(embeddingService.embedProject(PROJECT_ID, true))
                .thenReturn(new BatchEmbeddingResult(4, 3, 1, 0, "text-embedding-3-large", List.of()));

        mockMvc.perform(post("/api/v1/embeddings/batch")
                        .param("project", "ground-control")
                        .param("force", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(4)))
                .andExpect(jsonPath("$.embedded", is(3)))
                .andExpect(jsonPath("$.skipped", is(1)))
                .andExpect(jsonPath("$.failed", is(0)))
                .andExpect(jsonPath("$.errors", hasSize(0)));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(embeddingService).deleteEmbedding(REQUIREMENT_ID);

        mockMvc.perform(delete("/api/v1/embeddings/{requirementId}", REQUIREMENT_ID))
                .andExpect(status().isNoContent());

        verify(embeddingService).deleteEmbedding(eq(REQUIREMENT_ID));
        verify(projectService, org.mockito.Mockito.never()).resolveProjectId(any());
    }
}
