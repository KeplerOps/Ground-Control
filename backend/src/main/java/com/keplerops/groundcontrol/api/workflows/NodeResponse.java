package com.keplerops.groundcontrol.api.workflows;

import com.keplerops.groundcontrol.domain.workflows.model.WorkflowNode;
import com.keplerops.groundcontrol.domain.workflows.state.NodeType;
import java.time.Instant;
import java.util.UUID;

public record NodeResponse(
        UUID id,
        UUID workflowId,
        String name,
        String label,
        NodeType nodeType,
        String config,
        Integer positionX,
        Integer positionY,
        Integer timeoutSeconds,
        String retryPolicy,
        Instant createdAt,
        Instant updatedAt) {

    public static NodeResponse from(WorkflowNode n) {
        return new NodeResponse(
                n.getId(), n.getWorkflow().getId(), n.getName(), n.getLabel(),
                n.getNodeType(), n.getConfig(), n.getPositionX(), n.getPositionY(),
                n.getTimeoutSeconds(), n.getRetryPolicy(), n.getCreatedAt(), n.getUpdatedAt());
    }
}
