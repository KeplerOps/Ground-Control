package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment;
import com.keplerops.groundcontrol.domain.controls.state.ControlEffectivenessRating;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ControlEffectivenessAssessmentResponse(
        UUID id,
        String graphNodeId,
        String projectIdentifier,
        UUID controlId,
        String controlUid,
        String uid,
        ControlEffectivenessRating designEffectiveness,
        ControlEffectivenessRating operatingEffectiveness,
        LocalDate assessedAt,
        String assessor,
        String rationale,
        String notes,
        List<UUID> supportingTestIds,
        Instant createdAt,
        Instant updatedAt) {

    public static ControlEffectivenessAssessmentResponse from(ControlEffectivenessAssessment assessment) {
        return new ControlEffectivenessAssessmentResponse(
                assessment.getId(),
                GraphIds.nodeId(GraphEntityType.CONTROL_EFFECTIVENESS_ASSESSMENT, assessment.getId()),
                assessment.getProject().getIdentifier(),
                assessment.getControl().getId(),
                assessment.getControl().getUid(),
                assessment.getUid(),
                assessment.getDesignEffectiveness(),
                assessment.getOperatingEffectiveness(),
                assessment.getAssessedAt(),
                assessment.getAssessor(),
                assessment.getRationale(),
                assessment.getNotes(),
                assessment.getSupportingTestIds() == null
                        ? List.of()
                        : assessment.getSupportingTestIds().stream()
                                .map(UUID::fromString)
                                .toList(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt());
    }
}
