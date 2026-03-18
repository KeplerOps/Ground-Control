package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.ConsistencyViolation;
import java.util.UUID;

public record ConsistencyViolationResponse(
        UUID relationId,
        UUID sourceId,
        String sourceUid,
        String sourceStatus,
        UUID targetId,
        String targetUid,
        String targetStatus,
        String relationType,
        String violationType) {

    public static ConsistencyViolationResponse from(ConsistencyViolation v) {
        var rel = v.relation();
        return new ConsistencyViolationResponse(
                rel.getId(),
                rel.getSource().getId(),
                rel.getSource().getUid(),
                rel.getSource().getStatus().name(),
                rel.getTarget().getId(),
                rel.getTarget().getUid(),
                rel.getTarget().getStatus().name(),
                rel.getRelationType().name(),
                v.violationType());
    }
}
