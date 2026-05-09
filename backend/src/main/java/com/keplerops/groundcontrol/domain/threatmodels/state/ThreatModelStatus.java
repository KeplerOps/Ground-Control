package com.keplerops.groundcontrol.domain.threatmodels.state;

import java.util.Set;

/**
 * Content lifecycle for a threat model entry.
 *
 * <pre>
 * DRAFT ──► ACTIVE ──► ARCHIVED
 *    └──────────────────────► ARCHIVED
 * </pre>
 */
@SuppressWarnings("java:S125")
public enum ThreatModelStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED;

    public Set<ThreatModelStatus> validTargets() {
        return switch (this) {
            case DRAFT -> Set.of(ACTIVE, ARCHIVED);
            case ACTIVE -> Set.of(ARCHIVED);
            case ARCHIVED -> Set.of();
        };
    }

    public boolean canTransitionTo(ThreatModelStatus target) {
        return validTargets().contains(target);
    }
}
