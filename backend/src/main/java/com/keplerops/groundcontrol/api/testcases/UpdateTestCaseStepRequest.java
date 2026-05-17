package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Partial-update DTO for a test case step. Same convention as
 * {@link UpdateTestCaseRequest}: {@code null} means "no change"; the
 * {@code clearActualResult} flag means "set to null". {@code action} and
 * {@code expectedResult} are non-nullable on the entity, so they cannot be
 * cleared — passing {@code null} leaves them untouched, passing a blank value
 * is rejected by the entity-level validation.
 */
public record UpdateTestCaseStepRequest(
        @Positive Integer stepNumber,
        @Size(max = 10000) String action,
        @Size(max = 10000) String expectedResult,
        @Size(max = 10000) String actualResult,
        Boolean clearActualResult) {}
