package com.keplerops.groundcontrol.api.workflows;

import com.keplerops.groundcontrol.domain.workflows.model.WorkflowEdge;
import java.time.Instant;
import java.util.UUID;

public record EdgeResponse(
        UUID id,
        UUID workflowId,
        UUID sourceNodeId,
        String sourceNodeName,
        UUID targetNodeId,
        String targetNodeName,
        String conditionExpr,
        String label,
        Instant createdAt) {

    public static EdgeResponse from(WorkflowEdge e) {
        return new EdgeResponse(
                e.getId(), e.getWorkflow().getId(),
                e.getSourceNode().getId(), e.getSourceNode().getName(),
                e.getTargetNode().getId(), e.getTargetNode().getName(),
                e.getConditionExpr(), e.getLabel(), e.getCreatedAt());
    }
}
