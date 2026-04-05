package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlStatus;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ControlResponse(
        UUID id,
        String graphNodeId,
        String projectIdentifier,
        String uid,
        String title,
        String description,
        String objective,
        ControlFunction controlFunction,
        ControlStatus status,
        String owner,
        String implementationScope,
        Map<String, Object> methodologyFactors,
        Map<String, Object> effectiveness,
        String category,
        String source,
        Instant createdAt,
        Instant updatedAt) {

    public static ControlResponse from(Control control) {
        return new ControlResponse(
                control.getId(),
                GraphIds.nodeId(GraphEntityType.CONTROL, control.getId()),
                control.getProject().getIdentifier(),
                control.getUid(),
                control.getTitle(),
                control.getDescription(),
                control.getObjective(),
                control.getControlFunction(),
                control.getStatus(),
                control.getOwner(),
                control.getImplementationScope(),
                control.getMethodologyFactors(),
                control.getEffectiveness(),
                control.getCategory(),
                control.getSource(),
                control.getCreatedAt(),
                control.getUpdatedAt());
    }
}
