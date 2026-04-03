package com.keplerops.groundcontrol.domain.riskscenarios.state;

import java.util.Set;

public enum RiskAssessmentApprovalStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED;

    public Set<RiskAssessmentApprovalStatus> validTargets() {
        return switch (this) {
            case DRAFT -> Set.of(SUBMITTED);
            case SUBMITTED -> Set.of(APPROVED, REJECTED, DRAFT);
            case APPROVED -> Set.of(REJECTED);
            case REJECTED -> Set.of(DRAFT, SUBMITTED);
        };
    }

    public boolean canTransitionTo(RiskAssessmentApprovalStatus target) {
        return validTargets().contains(target);
    }
}
