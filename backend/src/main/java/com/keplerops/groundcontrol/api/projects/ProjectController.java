package com.keplerops.groundcontrol.api.projects;

import com.keplerops.groundcontrol.domain.projects.service.CreateProjectCommand;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.projects.service.UpdateProjectCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        var command = new CreateProjectCommand(request.identifier(), request.name(), request.description());
        return ProjectResponse.from(projectService.create(command));
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return projectService.list().stream().map(ProjectResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public ProjectResponse getByIdentifier(@PathVariable String identifier) {
        return ProjectResponse.from(projectService.getByIdentifier(identifier));
    }

    @PutMapping("/{identifier}")
    public ProjectResponse update(@PathVariable String identifier, @Valid @RequestBody UpdateProjectRequest request) {
        var command = new UpdateProjectCommand(request.name(), request.description());
        return ProjectResponse.from(projectService.updateByIdentifier(identifier, command));
    }
}
