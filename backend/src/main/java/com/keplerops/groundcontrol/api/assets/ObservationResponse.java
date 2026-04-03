package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import java.time.Instant;
import java.util.UUID;

public record ObservationResponse(
        UUID id,
        String graphNodeId,
        UUID assetId,
        String assetUid,
        ObservationCategory category,
        String observationKey,
        String observationValue,
        String source,
        Instant observedAt,
        Instant expiresAt,
        String confidence,
        String evidenceRef,
        Instant createdAt,
        Instant updatedAt) {

    public static ObservationResponse from(Observation o) {
        return new ObservationResponse(
                o.getId(),
                GraphIds.nodeId(GraphEntityType.OBSERVATION, o.getId()),
                o.getAsset().getId(),
                o.getAsset().getUid(),
                o.getCategory(),
                o.getObservationKey(),
                o.getObservationValue(),
                o.getSource(),
                o.getObservedAt(),
                o.getExpiresAt(),
                o.getConfidence(),
                o.getEvidenceRef(),
                o.getCreatedAt(),
                o.getUpdatedAt());
    }
}
