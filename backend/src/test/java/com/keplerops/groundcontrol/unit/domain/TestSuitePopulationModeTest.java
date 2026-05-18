package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.testcases.state.TestSuitePopulationMode;
import org.junit.jupiter.api.Test;

/**
 * Pins the declared order of {@link TestSuitePopulationMode} — the
 * mcp/ground-control/lib.js and frontend/src/types/api.ts mirrors depend
 * on it, so a silent reorder here would silently diverge the contract.
 */
class TestSuitePopulationModeTest {

    @Test
    void declaresThreeModesInExpectedOrder() {
        assertThat(TestSuitePopulationMode.values())
                .containsExactly(
                        TestSuitePopulationMode.STATIC,
                        TestSuitePopulationMode.REQUIREMENTS_BASED,
                        TestSuitePopulationMode.QUERY_BASED);
    }
}
