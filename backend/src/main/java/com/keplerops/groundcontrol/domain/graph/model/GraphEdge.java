package com.keplerops.groundcontrol.domain.graph.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record GraphEdge(
        String id,
        String edgeType,
        String sourceId,
        String targetId,
        GraphEntityType sourceEntityType,
        GraphEntityType targetEntityType,
        Map<String, Object> properties) {

    public GraphEdge {
        properties = properties == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }
}
