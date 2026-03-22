package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.api.admin.EmbeddingController;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.BatchEmbeddingResult;
import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingResult;
import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingService;
import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbeddingControllerTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ProjectService projectService;

    private EmbeddingController controller;

    @BeforeEach
    void setUp() {
        controller = new EmbeddingController(embeddingService, projectService);
    }

    @Test
    void embed_delegatesToService() {
        var reqId = UUID.randomUUID();
        var result = new EmbeddingResult(reqId, "embedded", "model-1", "hash123");
        when(embeddingService.embedRequirement(reqId)).thenReturn(result);

        var response = controller.embed(reqId);

        assertThat(response.requirementId()).isEqualTo(reqId);
        assertThat(response.status()).isEqualTo("embedded");
        assertThat(response.modelId()).isEqualTo("model-1");
    }

    @Test
    void getStatus_delegatesToService() {
        var reqId = UUID.randomUUID();
        var status = new EmbeddingStatus(reqId, true, false, false, "model-1", "model-1", Instant.now());
        when(embeddingService.getEmbeddingStatus(reqId)).thenReturn(status);

        var response = controller.getStatus(reqId);

        assertThat(response.requirementId()).isEqualTo(reqId);
        assertThat(response.hasEmbedding()).isTrue();
        assertThat(response.isStale()).isFalse();
    }

    @Test
    void embedProject_resolvesProjectAndDelegates() {
        var projectId = UUID.randomUUID();
        when(projectService.resolveProjectId("my-project")).thenReturn(projectId);
        when(embeddingService.embedProject(projectId, false))
                .thenReturn(new BatchEmbeddingResult(5, 3, 2, 0, "model-1", List.of()));

        var response = controller.embedProject("my-project", false);

        assertThat(response.total()).isEqualTo(5);
        assertThat(response.embedded()).isEqualTo(3);
        assertThat(response.skipped()).isEqualTo(2);
    }

    @Test
    void deleteEmbedding_delegatesToService() {
        var reqId = UUID.randomUUID();

        controller.deleteEmbedding(reqId);

        verify(embeddingService).deleteEmbedding(reqId);
    }
}
