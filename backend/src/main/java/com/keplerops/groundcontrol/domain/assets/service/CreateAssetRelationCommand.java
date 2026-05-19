package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import com.keplerops.groundcontrol.domain.assets.state.KnowledgeState;
import java.time.Instant;
import java.util.UUID;

public record CreateAssetRelationCommand(
        UUID targetId,
        AssetRelationType relationType,
        String description,
        String sourceSystem,
        String externalSourceId,
        Instant collectedAt,
        String confidence,
        // GC-M018 topology-edge knowledge state. Null = use the entity
        // initializer's default (CONFIRMED).
        KnowledgeState knowledgeState) {

    public CreateAssetRelationCommand(
            UUID targetId,
            AssetRelationType relationType,
            String description,
            String sourceSystem,
            String externalSourceId,
            Instant collectedAt,
            String confidence) {
        this(targetId, relationType, description, sourceSystem, externalSourceId, collectedAt, confidence, null);
    }
}
