package com.keplerops.groundcontrol.domain.riskscenarios.state;

import java.util.Set;

/**
 * Lifecycle status for a risk scenario.
 *
 * <pre>
 * DRAFT ──► IDENTIFIED ──► ASSESSED ──► TREATED ──► ACCEPTED ──► CLOSED
 *                │              │            │
 *                └──────────────┴────────────┴──────────► CLOSED
 * </pre>
 */
@SuppressWarnings("java:S125")
public enum RiskScenarioStatus {
    DRAFT,
    IDENTIFIED,
    ASSESSED,
    TREATED,
    ACCEPTED,
    CLOSED;

    public Set<RiskScenarioStatus> validTargets() {
        return switch (this) {
            case DRAFT -> Set.of(IDENTIFIED);
            case IDENTIFIED -> Set.of(ASSESSED, CLOSED);
            case ASSESSED -> Set.of(TREATED, CLOSED);
            case TREATED -> Set.of(ACCEPTED, CLOSED);
            case ACCEPTED -> Set.of(CLOSED);
            case CLOSED -> Set.of();
        };
    }

    public boolean canTransitionTo(RiskScenarioStatus target) {
        return validTargets().contains(target);
    }
}
