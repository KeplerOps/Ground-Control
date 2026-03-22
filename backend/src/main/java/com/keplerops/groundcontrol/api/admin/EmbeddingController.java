package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/embeddings")
public class EmbeddingController {

    private final EmbeddingService embeddingService;
    private final ProjectService projectService;

    public EmbeddingController(EmbeddingService embeddingService, ProjectService projectService) {
        this.embeddingService = embeddingService;
        this.projectService = projectService;
    }

    @PostMapping("/{requirementId}")
    public EmbeddingResultResponse embed(@PathVariable UUID requirementId) {
        return EmbeddingResultResponse.from(embeddingService.embedRequirement(requirementId));
    }

    @GetMapping("/{requirementId}/status")
    public EmbeddingStatusResponse getStatus(@PathVariable UUID requirementId) {
        return EmbeddingStatusResponse.from(embeddingService.getEmbeddingStatus(requirementId));
    }

    @PostMapping("/batch")
    public BatchEmbeddingResultResponse embedProject(
            @RequestParam(required = false) String project, @RequestParam(defaultValue = "false") boolean force) {
        var projectId = projectService.resolveProjectId(project);
        return BatchEmbeddingResultResponse.from(embeddingService.embedProject(projectId, force));
    }

    @DeleteMapping("/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmbedding(@PathVariable UUID requirementId) {
        embeddingService.deleteEmbedding(requirementId);
    }
}
