package com.keplerops.groundcontrol.domain.controlpacks.state;

import java.util.Set;

public enum ControlPackLifecycleState {
    INSTALLED,
    UPGRADED,
    DEPRECATED,
    REMOVED;

    public /*@ pure @*/ Set<ControlPackLifecycleState> validTargets() {
        return switch (this) {
            case INSTALLED -> Set.of(UPGRADED, DEPRECATED, REMOVED);
            case UPGRADED -> Set.of(UPGRADED, DEPRECATED, REMOVED);
            case DEPRECATED -> Set.of(REMOVED, UPGRADED);
            case REMOVED -> Set.of();
        };
    }

    public /*@ pure @*/ boolean canTransitionTo(ControlPackLifecycleState target) {
        return validTargets().contains(target);
    }
}
