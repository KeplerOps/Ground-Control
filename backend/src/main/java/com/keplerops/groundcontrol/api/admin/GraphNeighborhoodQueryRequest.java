package com.keplerops.groundcontrol.api.admin;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record GraphNeighborhoodQueryRequest(
        @NotEmpty List<String> rootNodeIds, Integer maxDepth, List<String> entityTypes) {

    public int resolvedMaxDepth() {
        return maxDepth == null ? 4 : maxDepth;
    }
}
