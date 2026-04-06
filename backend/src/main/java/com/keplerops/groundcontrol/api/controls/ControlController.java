package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.service.ControlService;
import com.keplerops.groundcontrol.domain.controls.service.CreateControlCommand;
import com.keplerops.groundcontrol.domain.controls.service.UpdateControlCommand;
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
@RequestMapping("/api/v1/controls")
public class ControlController {

    private final ControlService controlService;
    private final ProjectService projectService;

    public ControlController(ControlService controlService, ProjectService projectService) {
        this.controlService = controlService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ControlResponse create(
            @Valid @RequestBody ControlRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return ControlResponse.from(controlService.create(new CreateControlCommand(
                projectId,
                request.uid(),
                request.title(),
                request.controlFunction(),
                request.description(),
                request.objective(),
                request.owner(),
                request.implementationScope(),
                request.methodologyFactors(),
                request.effectiveness(),
                request.category(),
                request.source())));
    }

    @GetMapping
    public List<ControlResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return controlService.listByProject(projectId).stream()
                .map(ControlResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ControlResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlResponse.from(controlService.getById(projectId, id));
    }

    @GetMapping("/uid/{uid}")
    public ControlResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlResponse.from(controlService.getByUid(uid, projectId));
    }

    @PutMapping("/{id}")
    public ControlResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateControlRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlResponse.from(controlService.update(
                projectId,
                id,
                new UpdateControlCommand(
                        request.title(),
                        request.controlFunction(),
                        request.description(),
                        request.objective(),
                        request.owner(),
                        request.implementationScope(),
                        request.methodologyFactors(),
                        request.effectiveness(),
                        request.category(),
                        request.source())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        controlService.delete(projectId, id);
    }

    @PutMapping("/{id}/status")
    public ControlResponse transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ControlStatusTransitionRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlResponse.from(controlService.transitionStatus(projectId, id, request.status()));
    }
}
