package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.service.AssetSubgraphResult;
import java.util.List;

public record AssetSubgraphResponse(List<AssetResponse> assets, List<AssetRelationResponse> relations) {

    public static AssetSubgraphResponse from(AssetSubgraphResult result) {
        return new AssetSubgraphResponse(
                result.assets().stream().map(AssetResponse::from).toList(),
                result.relations().stream().map(AssetRelationResponse::from).toList());
    }
}
