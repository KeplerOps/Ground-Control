package com.keplerops.groundcontrol.api.executions;

import com.keplerops.groundcontrol.domain.executions.model.TaskExecution;
import com.keplerops.groundcontrol.domain.workflows.state.ExecutionStatus;
import com.keplerops.groundcontrol.domain.workflows.state.NodeType;
import java.time.Instant;
import java.util.UUID;

public record TaskExecutionResponse(
        UUID id,
        UUID executionId,
        UUID nodeId,
        String nodeName,
        NodeType nodeType,
        ExecutionStatus status,
        Integer attempt,
        String inputs,
        String outputs,
        String logs,
        String error,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        Instant createdAt) {

    public static TaskExecutionResponse from(TaskExecution t) {
        return new TaskExecutionResponse(
                t.getId(), t.getExecution().getId(),
                t.getNode() != null ? t.getNode().getId() : null,
                t.getNodeName(), t.getNodeType(), t.getStatus(), t.getAttempt(),
                t.getInputs(), t.getOutputs(), t.getLogs(), t.getError(),
                t.getStartedAt(), t.getFinishedAt(), t.getDurationMs(), t.getCreatedAt());
    }
}
