package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
import java.time.Instant;

public record CreateObservationCommand(
        ObservationCategory category,
        String observationKey,
        String observationValue,
        String source,
        Instant observedAt,
        Instant expiresAt,
        String confidence,
        String evidenceRef) {}
