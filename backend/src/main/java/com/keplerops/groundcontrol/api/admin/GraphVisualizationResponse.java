package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.api.requirements.RelationResponse;
import com.keplerops.groundcontrol.domain.requirements.service.GraphVisualizationResult;
import java.util.List;

public record GraphVisualizationResponse(
        List<GraphVisualizationNodeResponse> nodes, List<RelationResponse> edges, int totalNodes, int totalEdges) {

    public static GraphVisualizationResponse from(GraphVisualizationResult result) {
        return new GraphVisualizationResponse(
                result.requirements().stream()
                        .map(GraphVisualizationNodeResponse::from)
                        .toList(),
                result.relations().stream().map(RelationResponse::from).toList(),
                result.totalRequirements(),
                result.totalRelations());
    }
}
