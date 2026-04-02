package com.keplerops.groundcontrol.api.assets;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateObservationRequest(
        @Size(max = 65535) String observationValue,
        Instant expiresAt,
        @Size(max = 50) String confidence,
        @Size(max = 2000) String evidenceRef) {}
