package com.keplerops.groundcontrol.api.workflows;

import com.keplerops.groundcontrol.domain.workflows.model.Workflow;
import com.keplerops.groundcontrol.domain.workflows.state.WorkflowStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkflowResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String description,
        WorkflowStatus status,
        Integer currentVersion,
        String tags,
        Integer timeoutSeconds,
        Integer maxRetries,
        Integer retryBackoffMs,
        Instant createdAt,
        Instant updatedAt) {

    public static WorkflowResponse from(Workflow w) {
        return new WorkflowResponse(
                w.getId(), w.getWorkspace().getId(), w.getName(), w.getDescription(),
                w.getStatus(), w.getCurrentVersion(), w.getTags(), w.getTimeoutSeconds(),
                w.getMaxRetries(), w.getRetryBackoffMs(), w.getCreatedAt(), w.getUpdatedAt());
    }
}
