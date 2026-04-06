package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
import org.junit.jupiter.api.Test;

class RiskScenarioStatusTest {

    @Test
    void draftCanTransitionToActive() {
        assertThat(RiskScenarioStatus.DRAFT.canTransitionTo(RiskScenarioStatus.ACTIVE))
                .isTrue();
    }

    @Test
    void draftCanTransitionToArchived() {
        assertThat(RiskScenarioStatus.DRAFT.canTransitionTo(RiskScenarioStatus.ARCHIVED))
                .isTrue();
    }

    @Test
    void activeCanTransitionToArchived() {
        assertThat(RiskScenarioStatus.ACTIVE.canTransitionTo(RiskScenarioStatus.ARCHIVED))
                .isTrue();
    }

    @Test
    void archivedIsTerminal() {
        assertThat(RiskScenarioStatus.ARCHIVED.validTargets()).isEmpty();
    }

    @Test
    void cannotTransitionBackwards() {
        assertThat(RiskScenarioStatus.ACTIVE.canTransitionTo(RiskScenarioStatus.DRAFT))
                .isFalse();
        assertThat(RiskScenarioStatus.ARCHIVED.canTransitionTo(RiskScenarioStatus.DRAFT))
                .isFalse();
    }
}
