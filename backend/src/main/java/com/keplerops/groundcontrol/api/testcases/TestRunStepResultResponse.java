package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestRunStepResult;
import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
import java.time.Instant;
import java.util.UUID;

public record TestRunStepResultResponse(
        UUID id,
        UUID testRunCaseResultId,
        UUID testCaseStepId,
        int stepNumberSnapshot,
        String actionSnapshot,
        String expectedResultSnapshot,
        int snapshotOrder,
        TestRunCaseResultStatus status,
        String comment,
        Instant executedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static TestRunStepResultResponse from(TestRunStepResult result) {
        return new TestRunStepResultResponse(
                result.getId(),
                result.getTestRunCaseResult().getId(),
                result.getTestCaseStep().getId(),
                result.getStepNumberSnapshot(),
                result.getActionSnapshot(),
                result.getExpectedResultSnapshot(),
                result.getSnapshotOrder(),
                result.getStatus(),
                result.getComment(),
                result.getExecutedAt(),
                result.getCreatedAt(),
                result.getUpdatedAt());
    }
}
