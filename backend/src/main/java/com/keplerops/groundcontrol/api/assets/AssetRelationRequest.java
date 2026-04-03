package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record AssetRelationRequest(
        @NotNull UUID targetId,
        @NotNull AssetRelationType relationType,
        String description,
        @Size(max = 100) String sourceSystem,
        @Size(max = 500) String externalSourceId,
        Instant collectedAt,
        @Size(max = 50) String confidence) {}
