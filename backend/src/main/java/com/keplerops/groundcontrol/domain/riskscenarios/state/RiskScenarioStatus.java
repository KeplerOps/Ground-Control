package com.keplerops.groundcontrol.domain.riskscenarios.state;

import java.util.Set;

/**
 * Content lifecycle for a risk scenario.
 *
 * <pre>
 * DRAFT ──► ACTIVE ──► ARCHIVED
 *    └──────────────────────► ARCHIVED
 * </pre>
 */
@SuppressWarnings("java:S125")
public enum RiskScenarioStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED;

    public Set<RiskScenarioStatus> validTargets() {
        return switch (this) {
            case DRAFT -> Set.of(ACTIVE, ARCHIVED);
            case ACTIVE -> Set.of(ARCHIVED);
            case ARCHIVED -> Set.of();
        };
    }

    public boolean canTransitionTo(RiskScenarioStatus target) {
        return validTargets().contains(target);
    }
}
