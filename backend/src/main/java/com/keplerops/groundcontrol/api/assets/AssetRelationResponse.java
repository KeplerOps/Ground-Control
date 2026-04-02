package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import java.time.Instant;
import java.util.UUID;

public record AssetRelationResponse(
        UUID id,
        UUID sourceId,
        String sourceUid,
        UUID targetId,
        String targetUid,
        AssetRelationType relationType,
        String sourceSystem,
        String externalSourceId,
        Instant collectedAt,
        String confidence,
        Instant createdAt) {

    public static AssetRelationResponse from(AssetRelation r) {
        return new AssetRelationResponse(
                r.getId(),
                r.getSource().getId(),
                r.getSource().getUid(),
                r.getTarget().getId(),
                r.getTarget().getUid(),
                r.getRelationType(),
                r.getSourceSystem(),
                r.getExternalSourceId(),
                r.getCollectedAt(),
                r.getConfidence(),
                r.getCreatedAt());
    }
}
