package com.keplerops.groundcontrol.api.assets;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateAssetExternalIdRequest(Instant collectedAt, @Size(max = 50) String confidence) {}
