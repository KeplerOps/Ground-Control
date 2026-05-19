package com.keplerops.groundcontrol.domain.testcases.state;

/**
 * Authored test-case format. Distinct from {@link TestCaseType} (how a test is
 * performed) and distinct from the child step / Gherkin row identity. Set on
 * create and immutable thereafter so the parent aggregate cannot drift between
 * format families while children still exist.
 *
 * @see <a href="../../../../../../architecture/adrs/042-test-case-bdd-gherkin-format.md">ADR-042</a>
 */
public enum TestCaseFormat {
    /** Ordered step rows (action / expectedResult / actualResult) per ADR-041. */
    STEP_BASED,

    /** BDD/Gherkin .feature source per ADR-042. */
    GHERKIN
}
