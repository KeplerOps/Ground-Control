package com.keplerops.groundcontrol.api.assets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AssetExternalIdRequest(
        @NotBlank @Size(max = 100) String sourceSystem,
        @NotBlank @Size(max = 500) String sourceId,
        Instant collectedAt,
        @Size(max = 50) String confidence) {}
