package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.api.admin.PathResponse;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RequirementGraphController {

    private final GraphClient graphClient;
    private final ProjectService projectService;

    public RequirementGraphController(GraphClient graphClient, ProjectService projectService) {
        this.graphClient = graphClient;
        this.projectService = projectService;
    }

    @GetMapping("/api/v1/requirements/graph/ancestors/{uid}")
    public List<String> getAncestors(
            @PathVariable String uid,
            @RequestParam(defaultValue = "10") int depth,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return graphClient.getAncestors(projectId, uid, depth);
    }

    @GetMapping("/api/v1/requirements/graph/descendants/{uid}")
    public List<String> getDescendants(
            @PathVariable String uid,
            @RequestParam(defaultValue = "10") int depth,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return graphClient.getDescendants(projectId, uid, depth);
    }

    @GetMapping("/api/v1/requirements/graph/paths")
    public List<PathResponse> findPaths(
            @RequestParam String source, @RequestParam String target, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return graphClient.findPaths(projectId, source, target).stream()
                .map(PathResponse::from)
                .toList();
    }
}
