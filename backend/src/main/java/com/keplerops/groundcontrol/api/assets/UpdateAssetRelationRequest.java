package com.keplerops.groundcontrol.api.assets;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateAssetRelationRequest(
        String description,
        @Size(max = 100) String sourceSystem,
        @Size(max = 500) String externalSourceId,
        Instant collectedAt,
        @Size(max = 50) String confidence) {}
