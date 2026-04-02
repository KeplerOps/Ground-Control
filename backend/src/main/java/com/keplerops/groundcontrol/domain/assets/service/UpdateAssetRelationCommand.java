package com.keplerops.groundcontrol.domain.assets.service;

import java.time.Instant;

public record UpdateAssetRelationCommand(
        String description, String sourceSystem, String externalSourceId, Instant collectedAt, String confidence) {}
