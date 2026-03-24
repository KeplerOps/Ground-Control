package com.keplerops.groundcontrol.api.executions;

import com.keplerops.groundcontrol.domain.executions.model.Execution;
import com.keplerops.groundcontrol.domain.executions.model.TaskExecution;
import com.keplerops.groundcontrol.domain.workflows.state.ExecutionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExecutionDetailResponse(
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
        Instant createdAt,
        List<TaskExecutionResponse> tasks) {

    public static ExecutionDetailResponse from(Execution e, List<TaskExecution> tasks) {
        return new ExecutionDetailResponse(
                e.getId(), e.getWorkflow().getId(), e.getWorkflow().getName(),
                e.getWorkflowVersion(), e.getStatus(), e.getTriggerType(), e.getTriggerRef(),
                e.getInputs(), e.getOutputs(), e.getError(),
                e.getStartedAt(), e.getFinishedAt(), e.getDurationMs(), e.getCreatedAt(),
                tasks.stream().map(TaskExecutionResponse::from).toList());
    }
}
