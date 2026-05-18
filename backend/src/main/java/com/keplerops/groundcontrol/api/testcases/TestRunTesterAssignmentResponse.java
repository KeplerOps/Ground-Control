package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestRunTesterAssignment;
import java.time.Instant;
import java.util.UUID;

public record TestRunTesterAssignmentResponse(
        UUID id, UUID testRunId, String testerName, Instant createdAt, Instant updatedAt) {

    public static TestRunTesterAssignmentResponse from(TestRunTesterAssignment assignment) {
        return new TestRunTesterAssignmentResponse(
                assignment.getId(),
                assignment.getTestRun().getId(),
                assignment.getTesterName(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }
}
