package com.keplerops.groundcontrol.domain.assets.service;

import java.time.Instant;

public record UpdateAssetExternalIdCommand(Instant collectedAt, String confidence) {}
