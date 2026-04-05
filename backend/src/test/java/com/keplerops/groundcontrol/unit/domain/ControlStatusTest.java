package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.controls.state.ControlStatus;
import org.junit.jupiter.api.Test;

class ControlStatusTest {

    @Test
    void draftCanTransitionToProposed() {
        assertThat(ControlStatus.DRAFT.canTransitionTo(ControlStatus.PROPOSED)).isTrue();
    }

    @Test
    void draftCanTransitionToRetired() {
        assertThat(ControlStatus.DRAFT.canTransitionTo(ControlStatus.RETIRED)).isTrue();
    }

    @Test
    void draftCannotTransitionToImplemented() {
        assertThat(ControlStatus.DRAFT.canTransitionTo(ControlStatus.IMPLEMENTED))
                .isFalse();
    }

    @Test
    void proposedCanTransitionToImplemented() {
        assertThat(ControlStatus.PROPOSED.canTransitionTo(ControlStatus.IMPLEMENTED))
                .isTrue();
    }

    @Test
    void implementedCanTransitionToOperational() {
        assertThat(ControlStatus.IMPLEMENTED.canTransitionTo(ControlStatus.OPERATIONAL))
                .isTrue();
    }

    @Test
    void operationalCanTransitionToDeprecated() {
        assertThat(ControlStatus.OPERATIONAL.canTransitionTo(ControlStatus.DEPRECATED))
                .isTrue();
    }

    @Test
    void deprecatedCanTransitionToRetired() {
        assertThat(ControlStatus.DEPRECATED.canTransitionTo(ControlStatus.RETIRED))
                .isTrue();
    }

    @Test
    void deprecatedCanTransitionToOperational() {
        assertThat(ControlStatus.DEPRECATED.canTransitionTo(ControlStatus.OPERATIONAL))
                .isTrue();
    }

    @Test
    void retiredIsTerminal() {
        assertThat(ControlStatus.RETIRED.validTargets()).isEmpty();
    }
}
