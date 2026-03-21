package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.PathResult;
import java.util.ArrayList;
import java.util.List;

public record PathResponse(List<String> nodes, List<PathEdgeResponse> edges) {

    public record PathEdgeResponse(String sourceUid, String targetUid, String relationType) {}

    public static PathResponse from(PathResult result) {
        List<PathEdgeResponse> edges = new ArrayList<>();
        for (int i = 0; i < result.edgeLabels().size(); i++) {
            edges.add(new PathEdgeResponse(
                    result.nodeUids().get(i),
                    result.nodeUids().get(i + 1),
                    result.edgeLabels().get(i)));
        }
        return new PathResponse(result.nodeUids(), edges);
    }
}
