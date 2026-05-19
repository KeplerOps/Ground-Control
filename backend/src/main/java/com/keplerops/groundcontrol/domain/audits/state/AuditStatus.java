package com.keplerops.groundcontrol.domain.audits.state;

import java.util.Set;

public enum AuditStatus {
    PLANNED,
    IN_PROGRESS,
    DRAFT_REPORT,
    FINAL_REPORT,
    CLOSED;

    public Set<AuditStatus> validTargets() {
        return switch (this) {
            case PLANNED -> Set.of(IN_PROGRESS);
            case IN_PROGRESS -> Set.of(DRAFT_REPORT);
            case DRAFT_REPORT -> Set.of(FINAL_REPORT);
            case FINAL_REPORT -> Set.of(CLOSED, DRAFT_REPORT);
            case CLOSED -> Set.of();
        };
    }

    public boolean canTransitionTo(AuditStatus target) {
        return validTargets().contains(target);
    }
}
