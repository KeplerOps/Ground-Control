package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetCriticality;
import com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment;
import com.keplerops.groundcontrol.domain.assets.state.AssetScope;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import java.util.Map;
import java.util.UUID;

public record CreateAssetCommand(
        UUID projectId,
        String uid,
        String name,
        String description,
        AssetType assetType,
        String owner,
        String steward,
        AssetEnvironment environment,
        AssetCriticality criticality,
        String businessContext,
        AssetScope scopeDesignation,
        String subtype,
        Map<String, Object> metadata) {

    public CreateAssetCommand(UUID projectId, String uid, String name, String description, AssetType assetType) {
        this(projectId, uid, name, description, assetType, null, null, null, null, null, null, null, null);
    }

    public CreateAssetCommand(
            UUID projectId,
            String uid,
            String name,
            String description,
            AssetType assetType,
            String owner,
            String steward,
            AssetEnvironment environment,
            AssetCriticality criticality,
            String businessContext,
            AssetScope scopeDesignation) {
        this(
                projectId,
                uid,
                name,
                description,
                assetType,
                owner,
                steward,
                environment,
                criticality,
                businessContext,
                scopeDesignation,
                null,
                null);
    }
}
