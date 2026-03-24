package com.keplerops.groundcontrol.api.triggers;

import com.keplerops.groundcontrol.domain.triggers.model.Trigger;
import com.keplerops.groundcontrol.domain.workflows.state.TriggerType;
import java.time.Instant;
import java.util.UUID;

public record TriggerResponse(
        UUID id,
        UUID workflowId,
        String workflowName,
        String name,
        TriggerType triggerType,
        String config,
        boolean enabled,
        Instant lastFiredAt,
        Instant createdAt,
        Instant updatedAt) {

    public static TriggerResponse from(Trigger t) {
        return new TriggerResponse(
                t.getId(), t.getWorkflow().getId(), t.getWorkflow().getName(),
                t.getName(), t.getTriggerType(), t.getConfig(), t.isEnabled(),
                t.getLastFiredAt(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
