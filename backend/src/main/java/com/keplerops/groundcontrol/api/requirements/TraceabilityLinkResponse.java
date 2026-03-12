package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import com.keplerops.groundcontrol.domain.requirements.state.SyncStatus;
import java.time.Instant;
import java.util.UUID;

public record TraceabilityLinkResponse(
        UUID id,
        UUID requirementId,
        ArtifactType artifactType,
        String artifactIdentifier,
        String artifactUrl,
        String artifactTitle,
        LinkType linkType,
        SyncStatus syncStatus,
        Instant lastSyncedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static TraceabilityLinkResponse from(TraceabilityLink link) {
        return new TraceabilityLinkResponse(
                link.getId(),
                link.getRequirement().getId(),
                link.getArtifactType(),
                link.getArtifactIdentifier(),
                link.getArtifactUrl(),
                link.getArtifactTitle(),
                link.getLinkType(),
                link.getSyncStatus(),
                link.getLastSyncedAt(),
                link.getCreatedAt(),
                link.getUpdatedAt());
    }
}
