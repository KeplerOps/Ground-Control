package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import java.util.Map;

public record GraphEdgeResponse(
        String id,
        String edgeType,
        String sourceId,
        String targetId,
        String sourceEntityType,
        String targetEntityType,
        Map<String, Object> properties) {

    public static GraphEdgeResponse from(GraphEdge edge) {
        return new GraphEdgeResponse(
                edge.id(),
                edge.edgeType(),
                edge.sourceId(),
                edge.targetId(),
                edge.sourceEntityType().name(),
                edge.targetEntityType().name(),
                edge.properties());
    }
}
