package com.keplerops.groundcontrol.api.threatmodels;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.threatmodels.service.CreateThreatModelLinkCommand;
import com.keplerops.groundcontrol.domain.threatmodels.service.ThreatModelLinkService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/threat-models/{threatModelId}/links")
public class ThreatModelLinkController {

    private final ThreatModelLinkService linkService;
    private final ProjectService projectService;

    public ThreatModelLinkController(ThreatModelLinkService linkService, ProjectService projectService) {
        this.linkService = linkService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ThreatModelLinkResponse create(
            @PathVariable UUID threatModelId,
            @Valid @RequestBody ThreatModelLinkRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateThreatModelLinkCommand(
                request.targetType(),
                request.targetEntityId(),
                request.targetIdentifier(),
                request.linkType(),
                request.targetUrl(),
                request.targetTitle());
        return ThreatModelLinkResponse.from(linkService.create(projectId, threatModelId, command));
    }

    @GetMapping
    public List<ThreatModelLinkResponse> list(
            @PathVariable UUID threatModelId, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return linkService.listByThreatModel(projectId, threatModelId).stream()
                .map(ThreatModelLinkResponse::from)
                .toList();
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID threatModelId,
            @PathVariable UUID linkId,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        linkService.delete(projectId, threatModelId, linkId);
    }
}
