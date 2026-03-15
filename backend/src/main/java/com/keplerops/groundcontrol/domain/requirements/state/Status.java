package com.keplerops.groundcontrol.domain.requirements.state;

import java.util.Set;

/**
 * Lifecycle status for a requirement. Transitions are governed by a
 * switch-based state machine.
 *
 * <pre>
 * DRAFT ──► ACTIVE ──► DEPRECATED ──► ARCHIVED
 *                  └──────────────────►
 * </pre>
 */
@SuppressWarnings("java:S125") // JML contract annotations are intentional, not dead code
public enum Status {
    DRAFT,
    ACTIVE,
    DEPRECATED,
    ARCHIVED;

    /*@ ensures \result != null; @*/
    public /*@ pure @*/ Set<Status> validTargets() {
        return switch (this) {
            case DRAFT -> Set.of(ACTIVE);
            case ACTIVE -> Set.of(DEPRECATED, ARCHIVED);
            case DEPRECATED -> Set.of(ARCHIVED);
            case ARCHIVED -> Set.of();
        };
    }

    /*@ requires target != null; @*/
    public /*@ pure @*/ boolean canTransitionTo(Status target) {
        return validTargets().contains(target);
    }
}
