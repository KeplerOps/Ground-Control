package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import java.util.Set;
import java.util.UUID;

public interface MixedGraphClient {

    /**
     * Return the materialized project graph, filtered to the supplied entity types. An empty
     * {@code entityTypes} set means "no filter — every entity type". The contract requires the
     * implementation to enforce the {@link com.keplerops.groundcontrol.domain.graph.GraphTraversalLimits
     * MAX_PROJECTION_NODES / MAX_PROJECTION_EDGES} caps on the *filtered* result so a caller's
     * narrowing actually matters: the AGE adapter must apply the filter inside Cypher (so the
     * database stops materializing past the cap on the filtered set), and any in-memory fallback
     * must apply the filter before checking the cap.
     */
    GraphProjection getVisualization(UUID projectId, Set<GraphEntityType> entityTypes);
}
