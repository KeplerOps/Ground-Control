package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.state.AssetCriticality;
import com.keplerops.groundcontrol.domain.assets.state.AssetEnvironment;
import com.keplerops.groundcontrol.domain.assets.state.AssetScope;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.assets.state.KnowledgeState;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        String graphNodeId,
        String projectIdentifier,
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
        Map<String, Object> metadata,
        KnowledgeState knowledgeState,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static AssetResponse from(OperationalAsset asset) {
        return new AssetResponse(
                asset.getId(),
                GraphIds.nodeId(GraphEntityType.OPERATIONAL_ASSET, asset.getId()),
                asset.getProject().getIdentifier(),
                asset.getUid(),
                asset.getName(),
                asset.getDescription(),
                asset.getAssetType(),
                asset.getOwner(),
                asset.getSteward(),
                asset.getEnvironment(),
                asset.getCriticality(),
                asset.getBusinessContext(),
                asset.getScopeDesignation(),
                asset.getSubtype(),
                asset.getMetadata(),
                asset.getKnowledgeState(),
                asset.getArchivedAt(),
                asset.getCreatedAt(),
                asset.getUpdatedAt());
    }
}
