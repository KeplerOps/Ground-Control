package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.util.UUID;

public record GraphVisualizationNodeResponse(
        UUID id,
        String uid,
        String title,
        String statement,
        String priority,
        String status,
        String requirementType,
        Integer wave) {

    public static GraphVisualizationNodeResponse from(Requirement r) {
        return new GraphVisualizationNodeResponse(
                r.getId(),
                r.getUid(),
                r.getTitle(),
                r.getStatement(),
                r.getPriority().name(),
                r.getStatus().name(),
                r.getRequirementType().name(),
                r.getWave());
    }
}
