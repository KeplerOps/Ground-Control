package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.AnalysisService;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: split admin/read endpoints into separate controllers
@RestController
public class GraphController {

    private final GraphClient graphClient;
    private final AnalysisService analysisService;
    private final ProjectService projectService;

    public GraphController(GraphClient graphClient, AnalysisService analysisService, ProjectService projectService) {
        this.graphClient = graphClient;
        this.analysisService = analysisService;
        this.projectService = projectService;
    }

    @PostMapping("/api/v1/admin/graph/materialize")
    public void materializeGraph() {
        graphClient.materializeGraph();
    }

    @GetMapping("/api/v1/graph/ancestors/{uid}")
    public List<String> getAncestors(
            @PathVariable String uid,
            @RequestParam(defaultValue = "10") int depth,
            @RequestParam(required = false) String project) {
        if (project != null) {
            projectService.resolveProjectId(project);
        }
        return graphClient.getAncestors(uid, depth);
    }

    @GetMapping("/api/v1/graph/descendants/{uid}")
    public List<String> getDescendants(
            @PathVariable String uid,
            @RequestParam(defaultValue = "10") int depth,
            @RequestParam(required = false) String project) {
        if (project != null) {
            projectService.resolveProjectId(project);
        }
        return graphClient.getDescendants(uid, depth);
    }

    @GetMapping("/api/v1/graph/visualization")
    public GraphVisualizationResponse getVisualization(
            @RequestParam(required = false) String project, @RequestParam(required = false) List<String> entityTypes) {
        var projectId = projectService.resolveProjectId(project);
        var result = GraphVisualizationResponse.from(analysisService.getGraphVisualization(projectId));
        return filterByEntityTypes(result, entityTypes);
    }

    @GetMapping("/api/v1/graph/subgraph")
    public SubgraphResponse extractSubgraph(
            @RequestParam List<String> roots,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) List<String> entityTypes) {
        var projectId = projectService.resolveProjectId(project);
        var result = analysisService.extractSubgraph(projectId, roots);
        var response = SubgraphResponse.from(result, roots);
        if (entityTypes == null || entityTypes.isEmpty()) {
            return response;
        }
        var filteredNodes = response.nodes().stream()
                .filter(n -> entityTypes.contains(n.entityType()))
                .toList();
        return new SubgraphResponse(
                filteredNodes,
                response.edges(),
                filteredNodes.size(),
                response.edges().size(),
                roots);
    }

    private GraphVisualizationResponse filterByEntityTypes(
            GraphVisualizationResponse response, List<String> entityTypes) {
        if (entityTypes == null || entityTypes.isEmpty()) {
            return response;
        }
        var filteredNodes = response.nodes().stream()
                .filter(n -> entityTypes.contains(n.entityType()))
                .toList();
        return new GraphVisualizationResponse(
                filteredNodes,
                response.edges(),
                filteredNodes.size(),
                response.edges().size());
    }

    @GetMapping("/api/v1/graph/paths")
    public List<PathResponse> findPaths(
            @RequestParam String source, @RequestParam String target, @RequestParam(required = false) String project) {
        if (project != null) {
            projectService.resolveProjectId(project);
        }
        return graphClient.findPaths(source, target).stream()
                .map(PathResponse::from)
                .toList();
    }
}
