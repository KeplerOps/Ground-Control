package com.keplerops.groundcontrol.unit.domain.testcases.state;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.testcases.state.TestRunStatus;
import org.junit.jupiter.api.Test;

class TestRunStatusTest {

    @Test
    void plannedAllowsInProgressAbortedAndArchived() {
        assertThat(TestRunStatus.PLANNED.validTargets())
                .containsExactlyInAnyOrder(TestRunStatus.IN_PROGRESS, TestRunStatus.ABORTED, TestRunStatus.ARCHIVED);
    }

    @Test
    void inProgressAllowsCompletedAbortedAndArchived() {
        assertThat(TestRunStatus.IN_PROGRESS.validTargets())
                .containsExactlyInAnyOrder(TestRunStatus.COMPLETED, TestRunStatus.ABORTED, TestRunStatus.ARCHIVED);
    }

    @Test
    void completedAllowsOnlyArchived() {
        // A finished run is execution evidence: re-running is a new run, not
        // a status flip back to IN_PROGRESS. The only outbound arc is the
        // soft-delete to ARCHIVED.
        assertThat(TestRunStatus.COMPLETED.validTargets()).containsExactly(TestRunStatus.ARCHIVED);
    }

    @Test
    void abortedAllowsOnlyArchived() {
        // Same reasoning as COMPLETED — ABORTED is a terminal execution
        // outcome; reviving an aborted pass is a new run.
        assertThat(TestRunStatus.ABORTED.validTargets()).containsExactly(TestRunStatus.ARCHIVED);
    }

    @Test
    void archivedHasNoOutboundArcs() {
        assertThat(TestRunStatus.ARCHIVED.validTargets()).isEmpty();
    }

    @Test
    void canTransitionToValidatesEachArc() {
        assertThat(TestRunStatus.PLANNED.canTransitionTo(TestRunStatus.IN_PROGRESS))
                .isTrue();
        assertThat(TestRunStatus.PLANNED.canTransitionTo(TestRunStatus.COMPLETED))
                .isFalse();
        assertThat(TestRunStatus.IN_PROGRESS.canTransitionTo(TestRunStatus.PLANNED))
                .isFalse();
        assertThat(TestRunStatus.COMPLETED.canTransitionTo(TestRunStatus.IN_PROGRESS))
                .isFalse();
        assertThat(TestRunStatus.ARCHIVED.canTransitionTo(TestRunStatus.PLANNED))
                .isFalse();
    }

    @Test
    void canTransitionToRejectsNullTarget() {
        for (TestRunStatus status : TestRunStatus.values()) {
            assertThat(status.canTransitionTo(null)).isFalse();
        }
    }

    @Test
    void enumDeclarationOrderIsStable() {
        // Frontend / MCP mirrors iterate in declaration order; pin it.
        assertThat(TestRunStatus.values())
                .containsExactly(
                        TestRunStatus.PLANNED,
                        TestRunStatus.IN_PROGRESS,
                        TestRunStatus.COMPLETED,
                        TestRunStatus.ABORTED,
                        TestRunStatus.ARCHIVED);
    }
}
