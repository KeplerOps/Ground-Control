package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.model.AssetExternalId;
import java.time.Instant;
import java.util.UUID;

public record AssetExternalIdResponse(
        UUID id,
        UUID assetId,
        String assetUid,
        String sourceSystem,
        String sourceId,
        Instant collectedAt,
        String confidence,
        Instant createdAt,
        Instant updatedAt) {

    public static AssetExternalIdResponse from(AssetExternalId e) {
        return new AssetExternalIdResponse(
                e.getId(),
                e.getAsset().getId(),
                e.getAsset().getUid(),
                e.getSourceSystem(),
                e.getSourceId(),
                e.getCollectedAt(),
                e.getConfidence(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
