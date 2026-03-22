package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.api.requirements.RelationResponse;
import com.keplerops.groundcontrol.domain.requirements.service.SubgraphResult;
import java.util.List;

public record SubgraphResponse(
        List<GraphVisualizationNodeResponse> nodes,
        List<RelationResponse> edges,
        int totalNodes,
        int totalEdges,
        List<String> rootUids) {

    public static SubgraphResponse from(SubgraphResult result, List<String> rootUids) {
        var nodes = result.requirements().stream()
                .map(GraphVisualizationNodeResponse::from)
                .toList();
        var edges = result.relations().stream().map(RelationResponse::from).toList();
        return new SubgraphResponse(nodes, edges, nodes.size(), edges.size(), rootUids);
    }
}
