package com.keplerops.groundcontrol.domain.workflows.state;

import java.util.Set;

/**
 * Lifecycle status for a workflow definition.
 *
 * <pre>
 * DRAFT ──► ACTIVE ──► PAUSED ──► ARCHIVED
 *                  │          │
 *                  │   ◄──────┘
 *                  └──────────────► ARCHIVED
 * </pre>
 */
@SuppressWarnings("java:S125") // JML contract annotations are intentional, not dead code
public enum WorkflowStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    ARCHIVED;

    /*@ ensures \result != null; @*/
    public /*@ pure @*/ Set<WorkflowStatus> validTargets() {
        return switch (this) {
            case DRAFT -> Set.of(ACTIVE);
            case ACTIVE -> Set.of(PAUSED, ARCHIVED);
            case PAUSED -> Set.of(ACTIVE, ARCHIVED);
            case ARCHIVED -> Set.of();
        };
    }

    /*@ requires target != null; @*/
    public /*@ pure @*/ boolean canTransitionTo(WorkflowStatus target) {
        return validTargets().contains(target);
    }
}
