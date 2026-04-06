package com.keplerops.groundcontrol.api.admin;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record GraphPathsQueryRequest(
        @NotBlank String sourceNodeId, @NotBlank String targetNodeId, Integer maxDepth, List<String> entityTypes) {

    public int resolvedMaxDepth() {
        return maxDepth == null ? 6 : maxDepth;
    }
}
