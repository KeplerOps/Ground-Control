package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.graph.GraphTraversalLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GraphNeighborhoodQueryRequest(
        @NotEmpty @Size(max = GraphTraversalLimits.MAX_ROOT_NODES) List<@NotBlank @Size(max = GraphTraversalLimits.MAX_NODE_IDENTIFIER_LENGTH) String> rootNodeIds,
        @Min(1) @Max(GraphTraversalLimits.MAX_DEPTH) Integer maxDepth,
        @Size(max = GraphTraversalLimits.MAX_ENTITY_TYPE_FILTER) List<@NotBlank @Size(max = GraphTraversalLimits.MAX_NODE_IDENTIFIER_LENGTH) String> entityTypes) {

    public int resolvedMaxDepth() {
        return maxDepth == null ? 4 : maxDepth;
    }
}
