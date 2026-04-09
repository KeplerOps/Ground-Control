package com.keplerops.groundcontrol.api.controlpacks;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record UpgradeControlPackRequest(
        @NotBlank @Size(max = 200) String packId,
        @NotBlank @Size(max = 50) String newVersion,
        @Size(max = 200) String publisher,
        String description,
        @Size(max = 2000) String sourceUrl,
        @Size(max = 128) String checksum,
        Map<String, Object> compatibility,
        Map<String, Object> packMetadata,
        @NotNull @Size(min = 1) @Valid List<ControlPackEntryDefinitionRequest> entries) {}
