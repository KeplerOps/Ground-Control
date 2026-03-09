package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import java.time.Instant;
import java.util.UUID;

public record RelationResponse(
        UUID id,
        UUID sourceId,
        String sourceUid,
        UUID targetId,
        String targetUid,
        RelationType relationType,
        Instant createdAt) {

    public static RelationResponse from(RequirementRelation r) {
        return new RelationResponse(
                r.getId(),
                r.getSource().getId(),
                r.getSource().getUid(),
                r.getTarget().getId(),
                r.getTarget().getUid(),
                r.getRelationType(),
                r.getCreatedAt());
    }
}
