package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Structural verification of the Status transition table.
 * These are fast, deterministic tests (not property-based).
 */
class TransitionTablePropertiesTest {

    @Test
    void everyStatusHasTransitionEntry() {
        for (Status status : Status.values()) {
            // validTargets() should never return null
            assertThat(status.validTargets()).isNotNull();
        }
    }

    @Test
    void archivedIsTerminal() {
        assertThat(Status.ARCHIVED.validTargets()).isEmpty();
    }

    @Test
    void noSelfTransitions() {
        for (Status status : Status.values()) {
            assertThat(status.canTransitionTo(status))
                    .as("Status %s should not transition to itself", status)
                    .isFalse();
        }
    }

    @Test
    void draftIsEntryOnly() {
        // No status should transition TO draft
        Set<Status> statusesTargetingDraft = Arrays.stream(Status.values())
                .filter(s -> s.canTransitionTo(Status.DRAFT))
                .collect(Collectors.toSet());
        assertThat(statusesTargetingDraft).isEmpty();
    }

    @Test
    void allTargetsAreValidStatusValues() {
        Set<Status> allStatuses = Set.of(Status.values());
        for (Status status : Status.values()) {
            assertThat(allStatuses).containsAll(status.validTargets());
        }
    }
}
