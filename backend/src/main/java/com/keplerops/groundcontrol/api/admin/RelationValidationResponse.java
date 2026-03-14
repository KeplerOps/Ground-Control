package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import java.util.UUID;

public record RelationValidationResponse(
        UUID id,
        UUID sourceId,
        String sourceUid,
        Integer sourceWave,
        UUID targetId,
        String targetUid,
        Integer targetWave,
        String relationType) {

    public static RelationValidationResponse from(RequirementRelation r) {
        return new RelationValidationResponse(
                r.getId(),
                r.getSource().getId(),
                r.getSource().getUid(),
                r.getSource().getWave(),
                r.getTarget().getId(),
                r.getTarget().getUid(),
                r.getTarget().getWave(),
                r.getRelationType().name());
    }
}
