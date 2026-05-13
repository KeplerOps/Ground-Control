package com.keplerops.groundcontrol.domain.findings.state;

import java.util.Set;

public enum FindingStatus {
    OPEN,
    REMEDIATION_IN_PROGRESS,
    REMEDIATION_COMPLETE,
    VERIFIED_CLOSED;

    public Set<FindingStatus> validTargets() {
        return switch (this) {
            case OPEN -> Set.of(REMEDIATION_IN_PROGRESS);
            case REMEDIATION_IN_PROGRESS -> Set.of(REMEDIATION_COMPLETE);
            case REMEDIATION_COMPLETE -> Set.of(VERIFIED_CLOSED, REMEDIATION_IN_PROGRESS);
            case VERIFIED_CLOSED -> Set.of();
        };
    }

    public boolean canTransitionTo(FindingStatus target) {
        return validTargets().contains(target);
    }
}
