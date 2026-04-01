package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.service.AssetCycleEdge;
import com.keplerops.groundcontrol.domain.assets.service.AssetCycleResult;
import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import java.util.List;

public record AssetCycleResponse(List<String> memberUids, List<CycleEdgeResponse> edges) {

    public record CycleEdgeResponse(String sourceUid, String targetUid, AssetRelationType relationType) {

        public static CycleEdgeResponse from(AssetCycleEdge edge) {
            return new CycleEdgeResponse(edge.sourceUid(), edge.targetUid(), edge.relationType());
        }
    }

    public static AssetCycleResponse from(AssetCycleResult result) {
        return new AssetCycleResponse(
                result.memberUids(),
                result.edges().stream().map(CycleEdgeResponse::from).toList());
    }
}
