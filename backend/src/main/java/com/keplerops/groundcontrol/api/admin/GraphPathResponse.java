package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.graph.service.GraphPathResult;
import java.util.List;

public record GraphPathResponse(List<String> nodeIds, List<String> edgeTypes) {

    public static GraphPathResponse from(GraphPathResult result) {
        return new GraphPathResponse(result.nodeIds(), result.edgeTypes());
    }
}
