package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.service.ControlTestService;
import com.keplerops.groundcontrol.domain.controls.service.CreateControlTestCommand;
import com.keplerops.groundcontrol.domain.controls.service.UpdateControlTestCommand;
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
@RequestMapping("/api/v1/control-tests")
public class ControlTestController {

    private final ControlTestService controlTestService;
    private final ProjectService projectService;

    public ControlTestController(ControlTestService controlTestService, ProjectService projectService) {
        this.controlTestService = controlTestService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ControlTestResponse create(
            @Valid @RequestBody ControlTestRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return ControlTestResponse.from(controlTestService.create(new CreateControlTestCommand(
                projectId,
                request.controlId(),
                request.uid(),
                request.methodology(),
                request.testSteps(),
                request.expectedResults(),
                request.actualResults(),
                request.conclusion(),
                request.testerIdentity(),
                request.testDate(),
                request.notes())));
    }

    @GetMapping
    public List<ControlTestResponse> list(
            @RequestParam(required = false) UUID controlId, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var tests = controlId == null
                ? controlTestService.listByProject(projectId)
                : controlTestService.listByProjectAndControl(projectId, controlId);
        return tests.stream().map(ControlTestResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ControlTestResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlTestResponse.from(controlTestService.getById(projectId, id));
    }

    @PutMapping("/{id}")
    public ControlTestResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateControlTestRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlTestResponse.from(controlTestService.update(
                projectId,
                id,
                new UpdateControlTestCommand(
                        request.methodology(),
                        request.testSteps(),
                        request.expectedResults(),
                        request.actualResults(),
                        request.conclusion(),
                        request.testerIdentity(),
                        request.testDate(),
                        request.notes())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        controlTestService.delete(projectId, id);
    }
}
