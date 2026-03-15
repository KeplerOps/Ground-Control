package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.service.TraceabilityLinkRevision;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.SyncStatus;
import java.time.Instant;
import java.util.UUID;

public record TraceabilityLinkHistoryResponse(
        int revisionNumber, String revisionType, Instant timestamp, String actor, TraceabilityLinkSnapshot snapshot) {

    public record TraceabilityLinkSnapshot(
            UUID id,
            UUID requirementId,
            ArtifactType artifactType,
            String artifactIdentifier,
            String artifactUrl,
            String artifactTitle,
            LinkType linkType,
            SyncStatus syncStatus,
            Instant createdAt) {}

    public static TraceabilityLinkHistoryResponse from(TraceabilityLinkRevision revision) {
        var entity = revision.entity();
        var snapshot = new TraceabilityLinkSnapshot(
                entity.getId(),
                entity.getRequirement().getId(),
                entity.getArtifactType(),
                entity.getArtifactIdentifier(),
                entity.getArtifactUrl(),
                entity.getArtifactTitle(),
                entity.getLinkType(),
                entity.getSyncStatus(),
                entity.getCreatedAt());
        return new TraceabilityLinkHistoryResponse(
                revision.revisionNumber(), revision.revisionType(), revision.timestamp(), revision.actor(), snapshot);
    }
}
