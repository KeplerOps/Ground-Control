package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;

public record UpdateTestCaseCommand(
        String title,
        TestCaseType type,
        TestCasePriority priority,
        String description,
        String preconditions,
        String postconditions,
        Long estimatedDurationSeconds,
        boolean clearDescription,
        boolean clearPreconditions,
        boolean clearPostconditions,
        boolean clearEstimatedDuration) {}
