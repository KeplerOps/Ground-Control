package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestSuiteSourceRequirement;
import java.time.Instant;
import java.util.UUID;

public record TestSuiteSourceRequirementResponse(
        UUID id, UUID testSuiteId, UUID requirementId, String requirementUid, Instant createdAt, Instant updatedAt) {

    public static TestSuiteSourceRequirementResponse from(TestSuiteSourceRequirement source) {
        return new TestSuiteSourceRequirementResponse(
                source.getId(),
                source.getTestSuite().getId(),
                source.getRequirement().getId(),
                source.getRequirement().getUid(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }
}
