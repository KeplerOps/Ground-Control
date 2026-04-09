package com.keplerops.groundcontrol.domain.packregistry.state;

import java.util.Set;

public enum CatalogStatus {
    AVAILABLE,
    WITHDRAWN,
    SUPERSEDED;

    public /*@ pure @*/ Set<CatalogStatus> validTargets() {
        return switch (this) {
            case AVAILABLE -> Set.of(WITHDRAWN, SUPERSEDED);
            case WITHDRAWN -> Set.of();
            case SUPERSEDED -> Set.of();
        };
    }

    public /*@ pure @*/ boolean canTransitionTo(CatalogStatus target) {
        return validTargets().contains(target);
    }
}
