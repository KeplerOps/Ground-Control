package com.keplerops.groundcontrol.domain.controlpacks.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InstallControlPackCommand(
        UUID projectId,
        String packId,
        String version,
        String publisher,
        String description,
        String sourceUrl,
        String checksum,
        Map<String, Object> compatibility,
        Map<String, Object> packMetadata,
        List<ControlPackEntryDefinition> entries) {}
