package com.keplerops.groundcontrol.domain.testcases.state;

import java.util.Set;

/**
 * TC-006 / ADR-044 — Lifecycle status for a test plan.
 *
 * <p>The plan moves DRAFT → ACTIVE → IN_PROGRESS → COMPLETED in the common
 * case, with ARCHIVED as the terminal soft-delete state. Two backwards arcs
 * are allowed because real test efforts paginate: IN_PROGRESS may pause
 * back to ACTIVE, and COMPLETED may re-open to ACTIVE when late-arriving
 * runs need to be folded in. Once a plan is ARCHIVED it is terminal.
 */
public enum TestPlanStatus {
    DRAFT,
    ACTIVE,
    IN_PROGRESS,
    COMPLETED,
    ARCHIVED;

    public Set<TestPlanStatus> validTargets() {
        return switch (this) {
            case DRAFT -> Set.of(ACTIVE, ARCHIVED);
            case ACTIVE -> Set.of(IN_PROGRESS, COMPLETED, ARCHIVED);
            case IN_PROGRESS -> Set.of(ACTIVE, COMPLETED, ARCHIVED);
            case COMPLETED -> Set.of(ACTIVE, ARCHIVED);
            case ARCHIVED -> Set.of();
        };
    }

    public boolean canTransitionTo(TestPlanStatus target) {
        return target != null && validTargets().contains(target);
    }
}
