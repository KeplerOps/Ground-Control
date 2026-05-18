package com.keplerops.groundcontrol.unit.domain.testcases.state;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
import org.junit.jupiter.api.Test;

class TestRunCaseResultStatusTest {

    @Test
    void enumDeclarationOrderIsStable() {
        // Frontend / MCP mirrors iterate in declaration order; pin it so
        // the contract gate (ADR-034) and TypeScript array literal stay
        // honest.
        assertThat(TestRunCaseResultStatus.values())
                .containsExactly(
                        TestRunCaseResultStatus.NOT_RUN,
                        TestRunCaseResultStatus.PASSED,
                        TestRunCaseResultStatus.FAILED,
                        TestRunCaseResultStatus.BLOCKED,
                        TestRunCaseResultStatus.SKIPPED);
    }
}
