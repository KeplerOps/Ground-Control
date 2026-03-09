package com.keplerops.groundcontrol.domain.requirements.state;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle status for a requirement. Transitions are governed by a hand-rolled
 * state machine using an {@link EnumMap}.
 *
 * <pre>
 * DRAFT ──► ACTIVE ──► DEPRECATED ──► ARCHIVED
 *                  └──────────────────►
 * </pre>
 */
public enum Status {
    DRAFT,
    ACTIVE,
    DEPRECATED,
    ARCHIVED;

    private static final EnumMap<Status, Set<Status>> VALID_TRANSITIONS = new EnumMap<>(Map.of(
            DRAFT, Set.of(ACTIVE),
            ACTIVE, Set.of(DEPRECATED, ARCHIVED),
            DEPRECATED, Set.of(ARCHIVED),
            ARCHIVED, Set.of()));

    // @ ensures \result != null;
    // @ ensures VALID_TRANSITIONS.containsKey(this) ==> \result.equals(VALID_TRANSITIONS.get(this));
    public Set<Status> validTargets() {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of());
    }

    // @ requires target != null;
    // @ ensures \result <==> validTargets().contains(target);
    public boolean canTransitionTo(Status target) {
        return validTargets().contains(target);
    }
}
