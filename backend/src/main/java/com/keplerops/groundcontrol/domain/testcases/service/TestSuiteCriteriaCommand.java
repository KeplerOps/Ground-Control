package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.util.UUID;

/**
 * QUERY_BASED criteria payload shared by {@link CreateTestSuiteCommand}
 * and {@link UpdateTestSuiteCommand}. At least one field must be non-null
 * when the suite is QUERY_BASED — the service rejects an all-null
 * criteria block so the resolve path never produces the project's entire
 * test-case corpus by accident.
 */
public record TestSuiteCriteriaCommand(
        TestCaseStatus status,
        TestCaseType type,
        TestCasePriority priority,
        TestCaseFormat format,
        UUID folderId,
        String textSearch) {

    public static TestSuiteCriteriaCommand empty() {
        return new TestSuiteCriteriaCommand(null, null, null, null, null, null);
    }

    public boolean hasAny() {
        return status != null
                || type != null
                || priority != null
                || format != null
                || folderId != null
                || (textSearch != null && !textSearch.isBlank());
    }
}
