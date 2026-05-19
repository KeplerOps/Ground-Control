package com.keplerops.groundcontrol.domain.testcases.state;

import java.util.Set;

/**
 * TC-008 / ADR-049 — Lifecycle status for a test run.
 *
 * <p>A run is created in {@code PLANNED} once the suite has been snapshotted,
 * moves through {@code IN_PROGRESS} while testers record per-case results,
 * lands in {@code COMPLETED} when the team declares the pass finished, and
 * may divert to {@code ABORTED} if the pass is cancelled mid-flight (an
 * environment outage, a build pulled, a missed window). Any non-archived
 * state may be soft-deleted into {@code ARCHIVED}, which is terminal.
 *
 * <p>Unlike {@link TestPlanStatus}, there are no backwards arcs out of
 * {@code COMPLETED} or {@code ABORTED}: a run is a single execution pass
 * against a frozen snapshot, and re-running is a new run with its own
 * identity rather than a status flip on the prior one.
 */
public enum TestRunStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    ABORTED,
    ARCHIVED;

    public Set<TestRunStatus> validTargets() {
        return switch (this) {
            case PLANNED -> Set.of(IN_PROGRESS, ABORTED, ARCHIVED);
            case IN_PROGRESS -> Set.of(COMPLETED, ABORTED, ARCHIVED);
            case COMPLETED -> Set.of(ARCHIVED);
            case ABORTED -> Set.of(ARCHIVED);
            case ARCHIVED -> Set.of();
        };
    }

    public boolean canTransitionTo(TestRunStatus target) {
        return target != null && validTargets().contains(target);
    }
}
