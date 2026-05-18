package com.keplerops.groundcontrol.domain.testcases.state;

/**
 * TC-008 / ADR-049 — Per-case execution result on a {@code TestRun}.
 *
 * <p>Distinct from {@link TestCaseStatus} (which describes the authored
 * test-case lifecycle), {@link TestPlanStatus} (planning), and
 * {@link TestRunStatus} (the run aggregate's lifecycle). Each value is an
 * execution outcome for a single case in a single run.
 *
 * <p>Transitions between result states are not constrained — a tester may
 * mark a case PASSED after an initial FAILED on re-test, flip a BLOCKED to
 * SKIPPED if the test was descoped, etc. Validation is "value must be a
 * known enum constant" and nothing more.
 */
public enum TestRunCaseResultStatus {
    /** Snapshotted case has not yet been executed. Default on case-result creation. */
    NOT_RUN,
    PASSED,
    FAILED,
    BLOCKED,
    SKIPPED
}
