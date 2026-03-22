package com.keplerops.groundcontrol.api.baselines;

import com.keplerops.groundcontrol.domain.baselines.service.BaselineService;
import com.keplerops.groundcontrol.domain.baselines.service.CreateBaselineCommand;
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
@RequestMapping("/api/v1/baselines")
public class BaselineController {

    private final BaselineService baselineService;
    private final ProjectService projectService;

    public BaselineController(BaselineService baselineService, ProjectService projectService) {
        this.baselineService = baselineService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaselineResponse create(
            @Valid @RequestBody BaselineRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateBaselineCommand(projectId, request.name(), request.description());
        return BaselineResponse.from(baselineService.create(command));
    }

    @GetMapping
    public List<BaselineResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return baselineService.listByProject(projectId).stream()
                .map(BaselineResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public BaselineResponse getById(@PathVariable UUID id) {
        return BaselineResponse.from(baselineService.getById(id));
    }

    @GetMapping("/{id}/snapshot")
    public BaselineSnapshotResponse getSnapshot(@PathVariable UUID id) {
        return BaselineSnapshotResponse.from(baselineService.getSnapshot(id));
    }

    @GetMapping("/{id}/compare/{otherId}")
    public BaselineComparisonResponse compare(@PathVariable UUID id, @PathVariable UUID otherId) {
        return BaselineComparisonResponse.from(baselineService.compare(id, otherId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        baselineService.delete(id);
    }
}
