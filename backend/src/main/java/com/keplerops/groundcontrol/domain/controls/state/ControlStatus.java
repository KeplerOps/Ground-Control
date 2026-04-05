package com.keplerops.groundcontrol.domain.controls.state;

import java.util.Set;

public enum ControlStatus {
    DRAFT,
    PROPOSED,
    IMPLEMENTED,
    OPERATIONAL,
    DEPRECATED,
    RETIRED;

    public Set<ControlStatus> validTargets() {
        return switch (this) {
            case DRAFT -> Set.of(PROPOSED, RETIRED);
            case PROPOSED -> Set.of(IMPLEMENTED, RETIRED);
            case IMPLEMENTED -> Set.of(OPERATIONAL, DEPRECATED);
            case OPERATIONAL -> Set.of(DEPRECATED);
            case DEPRECATED -> Set.of(RETIRED, OPERATIONAL);
            case RETIRED -> Set.of();
        };
    }

    public boolean canTransitionTo(ControlStatus target) {
        return validTargets().contains(target);
    }
}
