package com.keplerops.groundcontrol.api.packregistry;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record UpdatePackRegistryEntryRequest(
        @Size(max = 200) String publisher,
        String description,
        @Size(max = 2000) String sourceUrl,
        @Size(max = 128) String checksum,
        Map<String, Object> signatureInfo,
        Map<String, Object> compatibility,
        List<Map<String, Object>> dependencies,
        Map<String, Object> provenance,
        Map<String, Object> registryMetadata) {}
