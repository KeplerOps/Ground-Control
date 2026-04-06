package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import java.util.List;

public record GraphVisualizationResponse(
        List<GraphVisualizationNodeResponse> nodes, List<GraphEdgeResponse> edges, int totalNodes, int totalEdges) {

    public static GraphVisualizationResponse from(GraphProjection result) {
        var nodes = GraphVisualizationNodeResponse.from(result);
        var edges = result.edges().stream().map(GraphEdgeResponse::from).toList();
        return new GraphVisualizationResponse(nodes, edges, nodes.size(), edges.size());
    }
}
