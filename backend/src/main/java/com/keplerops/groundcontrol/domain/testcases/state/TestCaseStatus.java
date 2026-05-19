package com.keplerops.groundcontrol.domain.testcases.state;

import java.util.Set;

public enum TestCaseStatus {
    DRAFT,
    APPROVED,
    DEPRECATED,
    ARCHIVED;

    public Set<TestCaseStatus> validTargets() {
        return switch (this) {
            case DRAFT -> Set.of(APPROVED, ARCHIVED);
            case APPROVED -> Set.of(DEPRECATED, ARCHIVED);
            case DEPRECATED -> Set.of(APPROVED, ARCHIVED);
            case ARCHIVED -> Set.of();
        };
    }

    public boolean canTransitionTo(TestCaseStatus target) {
        return target != null && validTargets().contains(target);
    }
}
