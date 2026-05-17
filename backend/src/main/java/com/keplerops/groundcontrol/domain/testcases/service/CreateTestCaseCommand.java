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
        Long estimatedDurationSeconds) {}
