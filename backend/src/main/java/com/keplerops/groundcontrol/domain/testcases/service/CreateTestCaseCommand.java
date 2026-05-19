package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.util.UUID;

public record CreateTestCaseCommand(
        UUID projectId,
        String uid,
        String title,
        TestCaseType type,
        TestCasePriority priority,
        TestCaseFormat format,
        String description,
        String preconditions,
        String postconditions,
        Long estimatedDurationSeconds,
        UUID parentFolderId,
        Integer sortOrder) {

    /** Backwards-compatible constructor for callers that do not yet specify placement. */
    public CreateTestCaseCommand(
            UUID projectId,
            String uid,
            String title,
            TestCaseType type,
            TestCasePriority priority,
            TestCaseFormat format,
            String description,
            String preconditions,
            String postconditions,
            Long estimatedDurationSeconds) {
        this(
                projectId,
                uid,
                title,
                type,
                priority,
                format,
                description,
                preconditions,
                postconditions,
                estimatedDurationSeconds,
                null,
                null);
    }
}
