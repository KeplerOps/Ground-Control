package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import java.util.List;

public record SubgraphResponse(
        List<GraphVisualizationNodeResponse> nodes,
        List<GraphEdgeResponse> edges,
        int totalNodes,
        int totalEdges,
        List<String> rootNodeIds) {

    public static SubgraphResponse from(GraphProjection result, List<String> rootNodeIds) {
        var nodes = GraphVisualizationNodeResponse.from(result);
        var edges = result.edges().stream().map(GraphEdgeResponse::from).toList();
        return new SubgraphResponse(nodes, edges, nodes.size(), edges.size(), rootNodeIds);
    }
}
