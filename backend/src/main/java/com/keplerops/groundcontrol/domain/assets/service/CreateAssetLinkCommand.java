package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkType;
import java.util.UUID;

public record CreateAssetLinkCommand(
        AssetLinkTargetType targetType,
        UUID targetEntityId,
        String targetIdentifier,
        AssetLinkType linkType,
        String targetUrl,
        String targetTitle) {}
