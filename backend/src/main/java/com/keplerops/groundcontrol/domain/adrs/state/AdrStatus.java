package com.keplerops.groundcontrol.domain.adrs.state;

import java.util.Set;

/**
 * Lifecycle status for an architecture decision record.
 *
 * <pre>
 * PROPOSED ──► ACCEPTED ──► DEPRECATED
 *                      └──► SUPERSEDED
 * </pre>
 */
@SuppressWarnings("java:S125")
public enum AdrStatus {
    PROPOSED,
    ACCEPTED,
    DEPRECATED,
    SUPERSEDED;

    /*@ ensures \result != null; @*/
    public /*@ pure @*/ Set<AdrStatus> validTargets() {
        return switch (this) {
            case PROPOSED -> Set.of(ACCEPTED);
            case ACCEPTED -> Set.of(DEPRECATED, SUPERSEDED);
            case DEPRECATED -> Set.of();
            case SUPERSEDED -> Set.of();
        };
    }

    /*@ requires target != null; @*/
    public /*@ pure @*/ boolean canTransitionTo(AdrStatus target) {
        return validTargets().contains(target);
    }
}
