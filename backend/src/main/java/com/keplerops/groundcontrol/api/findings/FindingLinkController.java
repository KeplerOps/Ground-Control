package com.keplerops.groundcontrol.api.findings;

import com.keplerops.groundcontrol.domain.findings.service.CreateFindingLinkCommand;
import com.keplerops.groundcontrol.domain.findings.service.FindingLinkService;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
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
@RequestMapping("/api/v1/findings/{findingId}/links")
public class FindingLinkController {

    private final FindingLinkService linkService;
    private final ProjectService projectService;

    public FindingLinkController(FindingLinkService linkService, ProjectService projectService) {
        this.linkService = linkService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FindingLinkResponse create(
            @PathVariable UUID findingId,
            @Valid @RequestBody FindingLinkRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateFindingLinkCommand(
                request.targetType(),
                request.targetEntityId(),
                request.targetIdentifier(),
                request.linkType(),
                request.targetUrl(),
                request.targetTitle());
        return FindingLinkResponse.from(linkService.create(projectId, findingId, command), findingId);
    }

    @GetMapping
    public List<FindingLinkResponse> list(
            @PathVariable UUID findingId, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return linkService.listByFinding(projectId, findingId).stream()
                .map(link -> FindingLinkResponse.from(link, findingId))
                .toList();
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID findingId, @PathVariable UUID linkId, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        linkService.delete(projectId, findingId, linkId);
    }
}
