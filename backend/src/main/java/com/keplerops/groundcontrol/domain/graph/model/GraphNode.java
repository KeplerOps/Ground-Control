package com.keplerops.groundcontrol.domain.graph.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record GraphNode(
        String id,
        String domainId,
        GraphEntityType entityType,
        String projectIdentifier,
        String uid,
        String label,
        Map<String, Object> properties) {

    public GraphNode {
        properties = properties == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }
}
