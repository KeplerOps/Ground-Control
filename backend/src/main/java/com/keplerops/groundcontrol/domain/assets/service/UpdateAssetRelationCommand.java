package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.KnowledgeState;
import java.time.Instant;

public record UpdateAssetRelationCommand(
        String description,
        String sourceSystem,
        String externalSourceId,
        Instant collectedAt,
        String confidence,
        // GC-M018 topology-edge knowledge state. Null = leave unchanged.
        // The underlying column is NOT NULL, so there is no clear flag.
        KnowledgeState knowledgeState) {

    public UpdateAssetRelationCommand(
            String description, String sourceSystem, String externalSourceId, Instant collectedAt, String confidence) {
        this(description, sourceSystem, externalSourceId, collectedAt, confidence, null);
    }
}
