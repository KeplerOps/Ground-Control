package com.keplerops.groundcontrol.domain.assets.service;

import java.time.Instant;

public record CreateAssetExternalIdCommand(
        String sourceSystem, String sourceId, Instant collectedAt, String confidence) {}
