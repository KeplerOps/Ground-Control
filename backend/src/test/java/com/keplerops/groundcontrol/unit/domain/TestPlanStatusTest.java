package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.testcases.state.TestPlanStatus;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TestPlanStatusTest {

    @Nested
    class DraftTransitions {

        @Test
        void canTransitionToActive() {
            assertThat(TestPlanStatus.DRAFT.canTransitionTo(TestPlanStatus.ACTIVE))
                    .isTrue();
        }

        @Test
        void canTransitionToArchived() {
            assertThat(TestPlanStatus.DRAFT.canTransitionTo(TestPlanStatus.ARCHIVED))
                    .isTrue();
        }

        @Test
        void cannotTransitionDirectlyToInProgress() {
            // DRAFT must reach IN_PROGRESS via ACTIVE: a freshly-authored plan
            // hasn't been published, so execution shouldn't begin from it.
            assertThat(TestPlanStatus.DRAFT.canTransitionTo(TestPlanStatus.IN_PROGRESS))
                    .isFalse();
        }

        @Test
        void cannotTransitionToCompleted() {
            assertThat(TestPlanStatus.DRAFT.canTransitionTo(TestPlanStatus.COMPLETED))
                    .isFalse();
        }

        @Test
        void cannotTransitionToSelf() {
            assertThat(TestPlanStatus.DRAFT.canTransitionTo(TestPlanStatus.DRAFT))
                    .isFalse();
        }
    }

    @Nested
    class ActiveTransitions {

        @Test
        void canTransitionToInProgress() {
            assertThat(TestPlanStatus.ACTIVE.canTransitionTo(TestPlanStatus.IN_PROGRESS))
                    .isTrue();
        }

        @Test
        void canTransitionToCompleted() {
            assertThat(TestPlanStatus.ACTIVE.canTransitionTo(TestPlanStatus.COMPLETED))
                    .isTrue();
        }

        @Test
        void canTransitionToArchived() {
            assertThat(TestPlanStatus.ACTIVE.canTransitionTo(TestPlanStatus.ARCHIVED))
                    .isTrue();
        }

        @Test
        void cannotTransitionBackToDraft() {
            assertThat(TestPlanStatus.ACTIVE.canTransitionTo(TestPlanStatus.DRAFT))
                    .isFalse();
        }

        @Test
        void cannotTransitionToSelf() {
            assertThat(TestPlanStatus.ACTIVE.canTransitionTo(TestPlanStatus.ACTIVE))
                    .isFalse();
        }
    }

    @Nested
    class InProgressTransitions {

        @Test
        void canTransitionBackToActive() {
            // Re-pause a run window without losing the plan: tests are paused,
            // schedule shifts, etc.
            assertThat(TestPlanStatus.IN_PROGRESS.canTransitionTo(TestPlanStatus.ACTIVE))
                    .isTrue();
        }

        @Test
        void canTransitionToCompleted() {
            assertThat(TestPlanStatus.IN_PROGRESS.canTransitionTo(TestPlanStatus.COMPLETED))
                    .isTrue();
        }

        @Test
        void canTransitionToArchived() {
            assertThat(TestPlanStatus.IN_PROGRESS.canTransitionTo(TestPlanStatus.ARCHIVED))
                    .isTrue();
        }

        @Test
        void cannotTransitionBackToDraft() {
            assertThat(TestPlanStatus.IN_PROGRESS.canTransitionTo(TestPlanStatus.DRAFT))
                    .isFalse();
        }

        @Test
        void cannotTransitionToSelf() {
            assertThat(TestPlanStatus.IN_PROGRESS.canTransitionTo(TestPlanStatus.IN_PROGRESS))
                    .isFalse();
        }
    }

    @Nested
    class CompletedTransitions {

        @Test
        void canReopenToActive() {
            // Late-arriving runs sometimes warrant re-opening a completed
            // plan; archive is the soft-delete path for "done forever".
            assertThat(TestPlanStatus.COMPLETED.canTransitionTo(TestPlanStatus.ACTIVE))
                    .isTrue();
        }

        @Test
        void canTransitionToArchived() {
            assertThat(TestPlanStatus.COMPLETED.canTransitionTo(TestPlanStatus.ARCHIVED))
                    .isTrue();
        }

        @Test
        void cannotTransitionBackToInProgress() {
            assertThat(TestPlanStatus.COMPLETED.canTransitionTo(TestPlanStatus.IN_PROGRESS))
                    .isFalse();
        }

        @Test
        void cannotTransitionToDraft() {
            assertThat(TestPlanStatus.COMPLETED.canTransitionTo(TestPlanStatus.DRAFT))
                    .isFalse();
        }

        @Test
        void cannotTransitionToSelf() {
            assertThat(TestPlanStatus.COMPLETED.canTransitionTo(TestPlanStatus.COMPLETED))
                    .isFalse();
        }
    }

    @Nested
    class ArchivedTransitions {

        @Test
        void hasNoValidTargets() {
            assertThat(TestPlanStatus.ARCHIVED.validTargets()).isEmpty();
        }

        @Test
        void cannotTransitionAnywhere() {
            for (TestPlanStatus target : TestPlanStatus.values()) {
                assertThat(TestPlanStatus.ARCHIVED.canTransitionTo(target))
                        .as("ARCHIVED -> %s", target)
                        .isFalse();
            }
        }
    }

    @Test
    void enumDeclaresExactlyTheFiveLifecycleStates() {
        assertThat(TestPlanStatus.values())
                .containsExactly(
                        TestPlanStatus.DRAFT,
                        TestPlanStatus.ACTIVE,
                        TestPlanStatus.IN_PROGRESS,
                        TestPlanStatus.COMPLETED,
                        TestPlanStatus.ARCHIVED);
    }

    @Test
    void canTransitionToNullIsAlwaysFalse() {
        for (TestPlanStatus from : TestPlanStatus.values()) {
            assertThat(from.canTransitionTo(null)).as("%s -> null", from).isFalse();
        }
    }

    @Test
    void validTargetsAreImmutable() {
        Set<TestPlanStatus> targets = TestPlanStatus.DRAFT.validTargets();
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> targets.add(TestPlanStatus.COMPLETED));
    }
}
