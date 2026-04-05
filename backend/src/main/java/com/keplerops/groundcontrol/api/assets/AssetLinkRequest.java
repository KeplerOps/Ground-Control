package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AssetLinkRequest(
        @NotNull AssetLinkTargetType targetType,
        UUID targetEntityId,
        @Size(max = 500) String targetIdentifier,
        @NotNull AssetLinkType linkType,
        @Size(max = 2000) String targetUrl,
        @Size(max = 255) String targetTitle) {}
