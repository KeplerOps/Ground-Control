package com.keplerops.groundcontrol.api.findings;

import com.keplerops.groundcontrol.domain.findings.service.CreateFindingCommand;
import com.keplerops.groundcontrol.domain.findings.service.FindingService;
import com.keplerops.groundcontrol.domain.findings.service.UpdateFindingCommand;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/findings")
public class FindingController {

    private final FindingService findingService;
    private final ProjectService projectService;

    public FindingController(FindingService findingService, ProjectService projectService) {
        this.findingService = findingService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FindingResponse create(
            @Valid @RequestBody FindingRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateFindingCommand(
                projectId,
                request.uid(),
                request.title(),
                request.findingType(),
                request.severity(),
                request.description(),
                request.rootCauseAnalysis(),
                request.owner(),
                request.dueDate());
        return FindingResponse.from(findingService.create(command));
    }

    @GetMapping
    public List<FindingResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return findingService.listByProject(projectId).stream()
                .map(FindingResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public FindingResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return FindingResponse.from(findingService.getById(projectId, id));
    }

    @GetMapping("/uid/{uid}")
    public FindingResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return FindingResponse.from(findingService.getByUid(uid, projectId));
    }

    @PutMapping("/{id}")
    public FindingResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFindingRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new UpdateFindingCommand(
                request.title(),
                request.findingType(),
                request.severity(),
                request.description(),
                request.rootCauseAnalysis(),
                request.owner(),
                request.dueDate(),
                Boolean.TRUE.equals(request.clearRootCauseAnalysis()),
                Boolean.TRUE.equals(request.clearOwner()),
                Boolean.TRUE.equals(request.clearDueDate()));
        return FindingResponse.from(findingService.update(projectId, id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        findingService.delete(projectId, id);
    }

    @PutMapping("/{id}/status")
    public FindingResponse transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody FindingStatusTransitionRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return FindingResponse.from(findingService.transitionStatus(projectId, id, request.status()));
    }
}
