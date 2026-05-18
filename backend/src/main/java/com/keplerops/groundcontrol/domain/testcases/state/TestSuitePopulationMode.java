package com.keplerops.groundcontrol.domain.testcases.state;

/**
 * TC-007 / ADR-047 — Population mode for a {@link
 * com.keplerops.groundcontrol.domain.testcases.model.TestSuite}.
 *
 * <p>The mode is a suite-level invariant set on create and immutable
 * thereafter (no setter on the entity). Each mode owns a different
 * population source — static suites own explicit member rows,
 * requirements-based suites own source-requirement rows resolved through
 * existing requirement/test traceability, query-based suites own typed
 * criteria resolved against the test-case repository at read time.
 * Switching modes would orphan the rows of the prior mode and break the
 * resolve-time dispatch contract.
 */
public enum TestSuitePopulationMode {
    /** Manually curated test-case membership held in {@code test_suite_member} rows. */
    STATIC,

    /**
     * Source requirements held in {@code test_suite_source_requirement} rows; member
     * test cases are resolved via {@code TraceabilityLink} (linkType = TESTS,
     * artifactType = TEST) at read time.
     */
    REQUIREMENTS_BASED,

    /**
     * Typed filter criteria (status / type / priority / format / folder /
     * text-search) held as columns on the suite root; member test cases are
     * resolved against the test-case repository at read time.
     */
    QUERY_BASED
}
