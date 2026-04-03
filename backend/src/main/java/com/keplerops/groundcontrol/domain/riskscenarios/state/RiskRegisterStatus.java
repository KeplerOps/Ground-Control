package com.keplerops.groundcontrol.domain.riskscenarios.state;

import java.util.Set;

public enum RiskRegisterStatus {
    IDENTIFIED,
    ANALYZING,
    ASSESSED,
    TREATING,
    MONITORING,
    ACCEPTED,
    CLOSED;

    public Set<RiskRegisterStatus> validTargets() {
        return switch (this) {
            case IDENTIFIED -> Set.of(ANALYZING, CLOSED);
            case ANALYZING -> Set.of(ASSESSED, CLOSED);
            case ASSESSED -> Set.of(TREATING, MONITORING, ACCEPTED, CLOSED);
            case TREATING -> Set.of(MONITORING, ACCEPTED, CLOSED);
            case MONITORING -> Set.of(TREATING, ACCEPTED, CLOSED);
            case ACCEPTED -> Set.of(MONITORING, CLOSED);
            case CLOSED -> Set.of();
        };
    }

    public boolean canTransitionTo(RiskRegisterStatus target) {
        return validTargets().contains(target);
    }
}
