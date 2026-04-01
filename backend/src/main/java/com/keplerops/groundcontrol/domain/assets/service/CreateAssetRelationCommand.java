package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import java.time.Instant;
import java.util.UUID;

public record CreateAssetRelationCommand(
        UUID targetId,
        AssetRelationType relationType,
        String sourceSystem,
        String externalSourceId,
        Instant collectedAt,
        String confidence) {}
