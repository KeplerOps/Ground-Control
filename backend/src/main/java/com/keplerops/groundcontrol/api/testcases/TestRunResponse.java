package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestRun;
import com.keplerops.groundcontrol.domain.testcases.state.TestRunStatus;
import java.time.Instant;
import java.util.UUID;

public record TestRunResponse(
        UUID id,
        String projectIdentifier,
        String uid,
        String name,
        UUID testPlanId,
        String testPlanUid,
        UUID testSuiteId,
        String testSuiteUid,
        String environment,
        String version,
        String build,
        TestRunStatus status,
        Instant startAt,
        Instant endAt,
        // TC-009 / ADR-050 — pause/resume cursor; nullable in both axes.
        UUID currentCaseResultId,
        UUID currentStepResultId,
        Instant createdAt,
        Instant updatedAt) {

    public static TestRunResponse from(TestRun run) {
        return new TestRunResponse(
                run.getId(),
                run.getProject().getIdentifier(),
                run.getUid(),
                run.getName(),
                run.getTestPlan().getId(),
                run.getTestPlan().getUid(),
                run.getTestSuite().getId(),
                run.getTestSuite().getUid(),
                run.getEnvironment(),
                run.getVersion(),
                run.getBuild(),
                run.getStatus(),
                run.getStartAt(),
                run.getEndAt(),
                run.getCurrentCaseResultId(),
                run.getCurrentStepResultId(),
                run.getCreatedAt(),
                run.getUpdatedAt());
    }
}
