package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestCaseStep;
import java.time.Instant;
import java.util.UUID;

public record TestCaseStepResponse(
        UUID id,
        UUID testCaseId,
        int stepNumber,
        String action,
        String expectedResult,
        String actualResult,
        Instant createdAt,
        Instant updatedAt) {

    public static TestCaseStepResponse from(TestCaseStep step) {
        return new TestCaseStepResponse(
                step.getId(),
                step.getTestCase().getId(),
                step.getStepNumber(),
                step.getAction(),
                step.getExpectedResult(),
                step.getActualResult(),
                step.getCreatedAt(),
                step.getUpdatedAt());
    }
}
