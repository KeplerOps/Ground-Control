package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkType;

public record CreateAssetLinkCommand(
        AssetLinkTargetType targetType,
        String targetIdentifier,
        AssetLinkType linkType,
        String targetUrl,
        String targetTitle) {}
