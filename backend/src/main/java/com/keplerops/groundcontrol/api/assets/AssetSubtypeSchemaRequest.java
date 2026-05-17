package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record AssetSubtypeSchemaRequest(
        @NotNull AssetType assetType,
        @NotBlank @Size(max = 100) String subtype,
        @NotBlank @Size(max = 50) String schemaVersion,
        String description,
        Map<String, Object> schemaBody) {}
