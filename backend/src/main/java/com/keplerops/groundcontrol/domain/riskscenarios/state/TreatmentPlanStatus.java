package com.keplerops.groundcontrol.domain.riskscenarios.state;

import java.util.Set;

public enum TreatmentPlanStatus {
    PLANNED,
    IN_PROGRESS,
    BLOCKED,
    COMPLETED,
    CANCELED;

    public Set<TreatmentPlanStatus> validTargets() {
        return switch (this) {
            case PLANNED -> Set.of(IN_PROGRESS, CANCELED);
            case IN_PROGRESS -> Set.of(BLOCKED, COMPLETED, CANCELED);
            case BLOCKED -> Set.of(IN_PROGRESS, CANCELED);
            case COMPLETED, CANCELED -> Set.of();
        };
    }

    public boolean canTransitionTo(TreatmentPlanStatus target) {
        return validTargets().contains(target);
    }
}
