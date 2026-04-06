package com.keplerops.groundcontrol.domain.graph.model;

import java.util.UUID;

public final class GraphIds {

    private GraphIds() {}

    public static String nodeId(GraphEntityType entityType, UUID domainId) {
        return entityType.name() + ":" + domainId;
    }
}
