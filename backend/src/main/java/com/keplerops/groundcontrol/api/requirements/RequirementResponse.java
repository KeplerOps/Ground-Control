package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RequirementType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.time.Instant;
import java.util.UUID;

public record RequirementResponse(
        UUID id,
        String uid,
        String title,
        String statement,
        String rationale,
        RequirementType requirementType,
        Priority priority,
        Status status,
        Integer wave,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt) {

    public static RequirementResponse from(Requirement r) {
        return new RequirementResponse(
                r.getId(),
                r.getUid(),
                r.getTitle(),
                r.getStatement(),
                r.getRationale(),
                r.getRequirementType(),
                r.getPriority(),
                r.getStatus(),
                r.getWave(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getArchivedAt());
    }
}
