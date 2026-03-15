package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.CycleResult;
import java.util.List;

public record CycleResponse(List<String> members, List<CycleEdgeResponse> edges) {

    public record CycleEdgeResponse(String sourceUid, String targetUid, String relationType) {}

    public static CycleResponse from(CycleResult result) {
        List<CycleEdgeResponse> edgeResponses = result.edges().stream()
                .map(e -> new CycleEdgeResponse(
                        e.sourceUid(), e.targetUid(), e.relationType().name()))
                .toList();
        return new CycleResponse(result.members(), edgeResponses);
    }
}
