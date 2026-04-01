package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.adrs.state.AdrStatus;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdrStatusTest {

    @Nested
    class ValidTargets {

        @Test
        void proposedCanTransitionToAccepted() {
            assertThat(AdrStatus.PROPOSED.validTargets()).isEqualTo(Set.of(AdrStatus.ACCEPTED));
        }

        @Test
        void acceptedCanTransitionToDeprecatedOrSuperseded() {
            assertThat(AdrStatus.ACCEPTED.validTargets()).isEqualTo(Set.of(AdrStatus.DEPRECATED, AdrStatus.SUPERSEDED));
        }

        @Test
        void deprecatedIsTerminal() {
            assertThat(AdrStatus.DEPRECATED.validTargets()).isEmpty();
        }

        @Test
        void supersededIsTerminal() {
            assertThat(AdrStatus.SUPERSEDED.validTargets()).isEmpty();
        }
    }

    @Nested
    class CanTransitionTo {

        @Test
        void proposedToAcceptedAllowed() {
            assertThat(AdrStatus.PROPOSED.canTransitionTo(AdrStatus.ACCEPTED)).isTrue();
        }

        @Test
        void proposedToDeprecatedNotAllowed() {
            assertThat(AdrStatus.PROPOSED.canTransitionTo(AdrStatus.DEPRECATED)).isFalse();
        }

        @Test
        void proposedToSupersededNotAllowed() {
            assertThat(AdrStatus.PROPOSED.canTransitionTo(AdrStatus.SUPERSEDED)).isFalse();
        }

        @Test
        void acceptedToDeprecatedAllowed() {
            assertThat(AdrStatus.ACCEPTED.canTransitionTo(AdrStatus.DEPRECATED)).isTrue();
        }

        @Test
        void acceptedToSupersededAllowed() {
            assertThat(AdrStatus.ACCEPTED.canTransitionTo(AdrStatus.SUPERSEDED)).isTrue();
        }

        @Test
        void acceptedToProposedNotAllowed() {
            assertThat(AdrStatus.ACCEPTED.canTransitionTo(AdrStatus.PROPOSED)).isFalse();
        }

        @Test
        void deprecatedToAnythingNotAllowed() {
            for (var target : AdrStatus.values()) {
                assertThat(AdrStatus.DEPRECATED.canTransitionTo(target)).isFalse();
            }
        }

        @Test
        void supersededToAnythingNotAllowed() {
            for (var target : AdrStatus.values()) {
                assertThat(AdrStatus.SUPERSEDED.canTransitionTo(target)).isFalse();
            }
        }
    }
}
