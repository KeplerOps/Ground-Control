package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.model.AssetLink;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkType;
import java.time.Instant;
import java.util.UUID;

public record AssetLinkResponse(
        UUID id,
        UUID assetId,
        String assetUid,
        AssetLinkTargetType targetType,
        String targetIdentifier,
        AssetLinkType linkType,
        String targetUrl,
        String targetTitle,
        Instant createdAt,
        Instant updatedAt) {

    public static AssetLinkResponse from(AssetLink link) {
        return new AssetLinkResponse(
                link.getId(),
                link.getAsset().getId(),
                link.getAsset().getUid(),
                link.getTargetType(),
                link.getTargetIdentifier(),
                link.getLinkType(),
                link.getTargetUrl(),
                link.getTargetTitle(),
                link.getCreatedAt(),
                link.getUpdatedAt());
    }
}
