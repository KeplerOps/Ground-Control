package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Partial-update DTO for a test suite. Mirrors {@code UpdateTestPlanRequest}:
 * null means "no change", matching {@code clearXxx: true} flags wipe a field
 * to null. {@code name} cannot be cleared. {@code populationMode} is
 * immutable; the controller does not expose it here.
 *
 * <p>The criteria fields apply only to QUERY_BASED suites; setting them
 * (or their clear flags) on a STATIC / REQUIREMENTS_BASED suite returns
 * HTTP 422 {@code invalid_test_suite_mode_field}.
 */
public record UpdateTestSuiteRequest(
        @Size(max = 200) String name,
        @Size(max = 8192) String description,
        // Codex pre-push cycle 2: PUT must reject populationMode rather than
        // silently accepting it as an unknown property. Binding it here with
        // @AssertNull makes the field part of the explicit contract and
        // produces HTTP 422 if a caller tries to flip a suite's mode.
        @Null(message = "populationMode is immutable; cannot be set via update") TestSuitePopulationMode populationMode,
        TestCaseStatus criteriaStatus,
        TestCaseType criteriaType,
        TestCasePriority criteriaPriority,
        TestCaseFormat criteriaFormat,
        UUID criteriaFolderId,
        @Size(max = 200) String criteriaTextSearch,
        Boolean clearDescription,
        Boolean clearCriteriaStatus,
        Boolean clearCriteriaType,
        Boolean clearCriteriaPriority,
        Boolean clearCriteriaFormat,
        Boolean clearCriteriaFolderId,
        Boolean clearCriteriaTextSearch) {}
