package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestRunCaseResult;
import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
import java.time.Instant;
import java.util.UUID;

public record TestRunCaseResultResponse(
        UUID id,
        UUID testRunId,
        UUID testCaseId,
        String testCaseUid,
        String testCaseTitle,
        int snapshotOrder,
        TestRunCaseResultStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static TestRunCaseResultResponse from(TestRunCaseResult result) {
        return new TestRunCaseResultResponse(
                result.getId(),
                result.getTestRun().getId(),
                result.getTestCase().getId(),
                result.getTestCaseUid(),
                result.getTestCaseTitle(),
                result.getSnapshotOrder(),
                result.getStatus(),
                result.getNotes(),
                result.getCreatedAt(),
                result.getUpdatedAt());
    }
}
