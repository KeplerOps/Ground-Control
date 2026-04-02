package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
import org.junit.jupiter.api.Test;

class RiskScenarioStatusTest {

    @Test
    void draftCanTransitionToIdentified() {
        assertThat(RiskScenarioStatus.DRAFT.canTransitionTo(RiskScenarioStatus.IDENTIFIED))
                .isTrue();
    }

    @Test
    void draftCannotSkipToAssessed() {
        assertThat(RiskScenarioStatus.DRAFT.canTransitionTo(RiskScenarioStatus.ASSESSED))
                .isFalse();
    }

    @Test
    void identifiedCanTransitionToAssessedOrClosed() {
        assertThat(RiskScenarioStatus.IDENTIFIED.canTransitionTo(RiskScenarioStatus.ASSESSED))
                .isTrue();
        assertThat(RiskScenarioStatus.IDENTIFIED.canTransitionTo(RiskScenarioStatus.CLOSED))
                .isTrue();
    }

    @Test
    void assessedCanTransitionToTreatedOrClosed() {
        assertThat(RiskScenarioStatus.ASSESSED.canTransitionTo(RiskScenarioStatus.TREATED))
                .isTrue();
        assertThat(RiskScenarioStatus.ASSESSED.canTransitionTo(RiskScenarioStatus.CLOSED))
                .isTrue();
    }

    @Test
    void treatedCanTransitionToAcceptedOrClosed() {
        assertThat(RiskScenarioStatus.TREATED.canTransitionTo(RiskScenarioStatus.ACCEPTED))
                .isTrue();
        assertThat(RiskScenarioStatus.TREATED.canTransitionTo(RiskScenarioStatus.CLOSED))
                .isTrue();
    }

    @Test
    void acceptedCanTransitionToClosed() {
        assertThat(RiskScenarioStatus.ACCEPTED.canTransitionTo(RiskScenarioStatus.CLOSED))
                .isTrue();
    }

    @Test
    void closedIsTerminal() {
        assertThat(RiskScenarioStatus.CLOSED.validTargets()).isEmpty();
    }

    @Test
    void cannotTransitionBackwards() {
        assertThat(RiskScenarioStatus.ASSESSED.canTransitionTo(RiskScenarioStatus.IDENTIFIED))
                .isFalse();
        assertThat(RiskScenarioStatus.CLOSED.canTransitionTo(RiskScenarioStatus.DRAFT))
                .isFalse();
    }
}
