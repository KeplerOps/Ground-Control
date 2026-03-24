package com.keplerops.groundcontrol.api.executions;

import com.keplerops.groundcontrol.domain.executions.model.Execution;
import com.keplerops.groundcontrol.domain.workflows.state.ExecutionStatus;
import java.time.Instant;
import java.util.UUID;

public record ExecutionResponse(
        UUID id,
        UUID workflowId,
        String workflowName,
        Integer workflowVersion,
        ExecutionStatus status,
        String triggerType,
        String triggerRef,
        String inputs,
        String outputs,
        String error,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        Instant createdAt) {

    public static ExecutionResponse from(Execution e) {
        return new ExecutionResponse(
                e.getId(), e.getWorkflow().getId(), e.getWorkflow().getName(),
                e.getWorkflowVersion(), e.getStatus(), e.getTriggerType(), e.getTriggerRef(),
                e.getInputs(), e.getOutputs(), e.getError(),
                e.getStartedAt(), e.getFinishedAt(), e.getDurationMs(), e.getCreatedAt());
    }
}
