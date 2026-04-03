package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ObservationRequest(
        @NotNull ObservationCategory category,
        @NotBlank @Size(max = 200) String observationKey,
        @NotBlank @Size(max = 65535) String observationValue,
        @NotBlank @Size(max = 200) String source,
        @NotNull Instant observedAt,
        Instant expiresAt,
        @Size(max = 50) String confidence,
        @Size(max = 2000) String evidenceRef) {}
