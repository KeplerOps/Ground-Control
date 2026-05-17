package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import java.util.Map;
import java.util.UUID;

public record CreateAssetSubtypeSchemaCommand(
        UUID projectId,
        AssetType assetType,
        String subtype,
        String schemaVersion,
        String description,
        Map<String, Object> schemaBody) {}
