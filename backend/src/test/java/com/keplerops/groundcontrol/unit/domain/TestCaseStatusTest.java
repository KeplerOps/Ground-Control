package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TestCaseStatusTest {

    @Nested
    class DraftTransitions {

        @Test
        void canTransitionToApproved() {
            assertThat(TestCaseStatus.DRAFT.canTransitionTo(TestCaseStatus.APPROVED))
                    .isTrue();
        }

        @Test
        void canTransitionToArchived() {
            assertThat(TestCaseStatus.DRAFT.canTransitionTo(TestCaseStatus.ARCHIVED))
                    .isTrue();
        }

        @Test
        void cannotTransitionToDeprecated() {
            assertThat(TestCaseStatus.DRAFT.canTransitionTo(TestCaseStatus.DEPRECATED))
                    .isFalse();
        }

        @Test
        void cannotTransitionToSelf() {
            assertThat(TestCaseStatus.DRAFT.canTransitionTo(TestCaseStatus.DRAFT))
                    .isFalse();
        }
    }

    @Nested
    class ApprovedTransitions {

        @Test
        void canTransitionToDeprecated() {
            assertThat(TestCaseStatus.APPROVED.canTransitionTo(TestCaseStatus.DEPRECATED))
                    .isTrue();
        }

        @Test
        void canTransitionToArchived() {
            assertThat(TestCaseStatus.APPROVED.canTransitionTo(TestCaseStatus.ARCHIVED))
                    .isTrue();
        }

        @Test
        void cannotTransitionToDraft() {
            assertThat(TestCaseStatus.APPROVED.canTransitionTo(TestCaseStatus.DRAFT))
                    .isFalse();
        }

        @Test
        void cannotTransitionToSelf() {
            assertThat(TestCaseStatus.APPROVED.canTransitionTo(TestCaseStatus.APPROVED))
                    .isFalse();
        }
    }

    @Nested
    class DeprecatedTransitions {

        @Test
        void canTransitionToApproved() {
            assertThat(TestCaseStatus.DEPRECATED.canTransitionTo(TestCaseStatus.APPROVED))
                    .isTrue();
        }

        @Test
        void canTransitionToArchived() {
            assertThat(TestCaseStatus.DEPRECATED.canTransitionTo(TestCaseStatus.ARCHIVED))
                    .isTrue();
        }

        @Test
        void cannotTransitionToDraft() {
            assertThat(TestCaseStatus.DEPRECATED.canTransitionTo(TestCaseStatus.DRAFT))
                    .isFalse();
        }

        @Test
        void cannotTransitionToSelf() {
            assertThat(TestCaseStatus.DEPRECATED.canTransitionTo(TestCaseStatus.DEPRECATED))
                    .isFalse();
        }
    }

    @Nested
    class ArchivedTransitions {

        @Test
        void hasNoValidTargets() {
            assertThat(TestCaseStatus.ARCHIVED.validTargets()).isEmpty();
        }

        @Test
        void cannotTransitionAnywhere() {
            for (TestCaseStatus target : TestCaseStatus.values()) {
                assertThat(TestCaseStatus.ARCHIVED.canTransitionTo(target))
                        .as("ARCHIVED -> %s", target)
                        .isFalse();
            }
        }
    }

    @Test
    void enumDeclaresExactlyTheFourLifecycleStates() {
        assertThat(TestCaseStatus.values())
                .containsExactly(
                        TestCaseStatus.DRAFT,
                        TestCaseStatus.APPROVED,
                        TestCaseStatus.DEPRECATED,
                        TestCaseStatus.ARCHIVED);
    }

    @Test
    void canTransitionToNullIsAlwaysFalse() {
        for (TestCaseStatus from : TestCaseStatus.values()) {
            assertThat(from.canTransitionTo(null)).as("%s -> null", from).isFalse();
        }
    }

    @Test
    void validTargetsAreImmutable() {
        Set<TestCaseStatus> targets = TestCaseStatus.DRAFT.validTargets();
        // Defensive: returned set must not allow mutation by callers.
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> targets.add(TestCaseStatus.DEPRECATED));
    }
}
