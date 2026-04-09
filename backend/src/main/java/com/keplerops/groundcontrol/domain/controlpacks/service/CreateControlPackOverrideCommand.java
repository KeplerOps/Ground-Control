package com.keplerops.groundcontrol.domain.controlpacks.service;

import java.util.UUID;

public record CreateControlPackOverrideCommand(
        UUID controlPackEntryId, String fieldName, String overrideValue, String reason) {}
