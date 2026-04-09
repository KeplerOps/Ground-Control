package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RegisterPackCommand(
        UUID projectId,
        String packId,
        PackType packType,
        String version,
        String publisher,
        String description,
        String sourceUrl,
        String checksum,
        Map<String, Object> signatureInfo,
        Map<String, Object> compatibility,
        List<Map<String, Object>> dependencies,
        Map<String, Object> provenance,
        Map<String, Object> registryMetadata) {}
