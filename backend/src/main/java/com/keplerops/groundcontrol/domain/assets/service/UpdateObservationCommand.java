package com.keplerops.groundcontrol.domain.assets.service;

import java.time.Instant;

public record UpdateObservationCommand(
        String observationValue, Instant expiresAt, String confidence, String evidenceRef) {}
