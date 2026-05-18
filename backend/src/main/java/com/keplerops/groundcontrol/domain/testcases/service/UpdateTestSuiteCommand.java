package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import java.util.UUID;

/**
 * Partial-update payload for {@link TestSuiteService#update}. Follows the
 * {@code UpdateTestPlanCommand} convention (TC-001 codex-cycle-1
 * contract): null leaves a field alone, the matching {@code clear*} flag
 * wipes it.
 *
 * <p>{@code name} cannot be cleared (the entity requires it).
 * Criteria fields apply only to QUERY_BASED suites and are rejected
 * outright on STATIC / REQUIREMENTS_BASED suites — the service surfaces
 * that as an {@code invalid_test_suite_mode_field} validation error.
 * The all-null guard on QUERY_BASED suites is checked after applying the
 * patch so an update that would leave a QUERY_BASED suite without any
 * criterion is rejected.
 */
public record UpdateTestSuiteCommand(
        String name,
        String description,
        TestCaseStatus criteriaStatus,
        TestCaseType criteriaType,
        TestCasePriority criteriaPriority,
        TestCaseFormat criteriaFormat,
        UUID criteriaFolderId,
        String criteriaTextSearch,
        boolean clearDescription,
        boolean clearCriteriaStatus,
        boolean clearCriteriaType,
        boolean clearCriteriaPriority,
        boolean clearCriteriaFormat,
        boolean clearCriteriaFolderId,
        boolean clearCriteriaTextSearch) {}
