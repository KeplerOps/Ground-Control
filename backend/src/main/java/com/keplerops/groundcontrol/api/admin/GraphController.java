package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.graph.service.MixedGraphService;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.GraphClient;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GraphController {

    private final GraphClient graphClient;
    private final MixedGraphService mixedGraphService;
    private final ProjectService projectService;

    public GraphController(
            GraphClient graphClient, MixedGraphService mixedGraphService, ProjectService projectService) {
        this.graphClient = graphClient;
        this.mixedGraphService = mixedGraphService;
        this.projectService = projectService;
    }

    @PostMapping("/api/v1/admin/graph/materialize")
    public void materializeGraph() {
        graphClient.materializeGraph();
    }

    @GetMapping("/api/v1/graph/visualization")
    public GraphVisualizationResponse getVisualization(
            @RequestParam(required = false) String project, @RequestParam(required = false) List<String> entityTypes) {
        var projectId = projectService.requireProjectId(project);
        return GraphVisualizationResponse.from(mixedGraphService.getVisualization(projectId, entityTypes));
    }

    @PostMapping("/api/v1/graph/subgraph/query")
    public SubgraphResponse extractSubgraph(
            @Valid @RequestBody GraphNeighborhoodQueryRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return SubgraphResponse.from(
                mixedGraphService.extractSubgraph(
                        projectId, request.rootNodeIds(), request.resolvedMaxDepth(), request.entityTypes()),
                request.rootNodeIds());
    }

    @PostMapping("/api/v1/graph/traversal/query")
    public SubgraphResponse traverse(
            @Valid @RequestBody GraphNeighborhoodQueryRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return SubgraphResponse.from(
                mixedGraphService.traverse(
                        projectId, request.rootNodeIds(), request.resolvedMaxDepth(), request.entityTypes()),
                request.rootNodeIds());
    }

    @PostMapping("/api/v1/graph/paths/query")
    public List<GraphPathResponse> findPaths(
            @Valid @RequestBody GraphPathsQueryRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return mixedGraphService
                .findPaths(
                        projectId,
                        request.sourceNodeId(),
                        request.targetNodeId(),
                        request.resolvedMaxDepth(),
                        request.entityTypes())
                .stream()
                .map(GraphPathResponse::from)
                .toList();
    }
}
