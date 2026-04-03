package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import java.time.Instant;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        String graphNodeId,
        String projectIdentifier,
        String uid,
        String name,
        String description,
        AssetType assetType,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static AssetResponse from(OperationalAsset asset) {
        return new AssetResponse(
                asset.getId(),
                GraphIds.nodeId(GraphEntityType.OPERATIONAL_ASSET, asset.getId()),
                asset.getProject().getIdentifier(),
                asset.getUid(),
                asset.getName(),
                asset.getDescription(),
                asset.getAssetType(),
                asset.getArchivedAt(),
                asset.getCreatedAt(),
                asset.getUpdatedAt());
    }
}
