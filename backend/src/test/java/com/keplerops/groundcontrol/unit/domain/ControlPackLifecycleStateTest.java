package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.controlpacks.state.ControlPackLifecycleState;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ControlPackLifecycleStateTest {

    @Test
    void installedCanTransitionToUpgradedDeprecatedRemoved() {
        assertThat(ControlPackLifecycleState.INSTALLED.validTargets())
                .isEqualTo(Set.of(
                        ControlPackLifecycleState.UPGRADED,
                        ControlPackLifecycleState.DEPRECATED,
                        ControlPackLifecycleState.REMOVED));
    }

    @Test
    void upgradedCanTransitionToUpgradedDeprecatedRemoved() {
        assertThat(ControlPackLifecycleState.UPGRADED.validTargets())
                .isEqualTo(Set.of(
                        ControlPackLifecycleState.UPGRADED,
                        ControlPackLifecycleState.DEPRECATED,
                        ControlPackLifecycleState.REMOVED));
    }

    @Test
    void deprecatedCanTransitionToRemovedOrUpgraded() {
        assertThat(ControlPackLifecycleState.DEPRECATED.validTargets())
                .isEqualTo(Set.of(ControlPackLifecycleState.REMOVED, ControlPackLifecycleState.UPGRADED));
    }

    @Test
    void removedIsTerminal() {
        assertThat(ControlPackLifecycleState.REMOVED.validTargets()).isEmpty();
    }

    @Test
    void canTransitionToReturnsTrueForValidTransitions() {
        assertThat(ControlPackLifecycleState.INSTALLED.canTransitionTo(ControlPackLifecycleState.UPGRADED))
                .isTrue();
        assertThat(ControlPackLifecycleState.DEPRECATED.canTransitionTo(ControlPackLifecycleState.UPGRADED))
                .isTrue();
    }

    @Test
    void canTransitionToReturnsFalseForInvalidTransitions() {
        assertThat(ControlPackLifecycleState.REMOVED.canTransitionTo(ControlPackLifecycleState.INSTALLED))
                .isFalse();
        assertThat(ControlPackLifecycleState.INSTALLED.canTransitionTo(ControlPackLifecycleState.INSTALLED))
                .isFalse();
    }
}
