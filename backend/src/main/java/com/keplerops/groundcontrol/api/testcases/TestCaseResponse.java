package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.time.Instant;
import java.util.UUID;

public record TestCaseResponse(
        UUID id,
        String projectIdentifier,
        String uid,
        String title,
        String description,
        String preconditions,
        String postconditions,
        TestCasePriority priority,
        TestCaseStatus status,
        TestCaseType type,
        TestCaseFormat format,
        Long estimatedDurationSeconds,
        UUID parentFolderId,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt) {

    public static TestCaseResponse from(TestCase testCase) {
        return new TestCaseResponse(
                testCase.getId(),
                testCase.getProject().getIdentifier(),
                testCase.getUid(),
                testCase.getTitle(),
                testCase.getDescription(),
                testCase.getPreconditions(),
                testCase.getPostconditions(),
                testCase.getPriority(),
                testCase.getStatus(),
                testCase.getType(),
                testCase.getFormat(),
                testCase.getEstimatedDurationSeconds(),
                testCase.getParentFolder() != null ? testCase.getParentFolder().getId() : null,
                testCase.getSortOrder(),
                testCase.getCreatedAt(),
                testCase.getUpdatedAt());
    }
}
