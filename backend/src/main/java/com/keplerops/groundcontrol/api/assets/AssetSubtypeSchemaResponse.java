package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.model.AssetSubtypeSchema;
import com.keplerops.groundcontrol.domain.assets.state.AssetSubtypeSchemaStatus;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AssetSubtypeSchemaResponse(
        UUID id,
        String projectIdentifier,
        AssetType assetType,
        String subtype,
        String schemaVersion,
        String description,
        Map<String, Object> schemaBody,
        AssetSubtypeSchemaStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static AssetSubtypeSchemaResponse from(AssetSubtypeSchema schema) {
        return new AssetSubtypeSchemaResponse(
                schema.getId(),
                schema.getProject().getIdentifier(),
                schema.getAssetType(),
                schema.getSubtype(),
                schema.getSchemaVersion(),
                schema.getDescription(),
                schema.getSchemaBody(),
                schema.getStatus(),
                schema.getCreatedAt(),
                schema.getUpdatedAt());
    }
}
