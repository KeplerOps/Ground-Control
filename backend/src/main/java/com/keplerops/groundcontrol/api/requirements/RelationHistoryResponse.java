package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.service.RelationRevision;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.time.Instant;
import java.util.UUID;

public record RelationHistoryResponse(
        int revisionNumber,
        String revisionType,
        Instant timestamp,
        String actor,
        String reason,
        RelationSnapshot snapshot) {

    public record RelationSnapshot(
            UUID id, UUID sourceId, UUID targetId, RelationType relationType, String description, Instant createdAt) {}

    public static RelationHistoryResponse from(RelationRevision revision) {
        var entity = revision.entity();
        var snapshot = new RelationSnapshot(
                entity.getId(),
                entity.getSource().getId(),
                entity.getTarget().getId(),
                entity.getRelationType(),
                entity.getDescription(),
                entity.getCreatedAt());
        return new RelationHistoryResponse(
                revision.revisionNumber(),
                revision.revisionType(),
                revision.timestamp(),
                revision.actor(),
                revision.reason(),
                snapshot);
    }
}
