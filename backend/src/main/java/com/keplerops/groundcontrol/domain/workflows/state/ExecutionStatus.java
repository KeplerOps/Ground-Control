package com.keplerops.groundcontrol.domain.workflows.state;

import java.util.Set;

/**
 * Execution lifecycle status for workflow runs and task executions.
 *
 * <pre>
 * PENDING ──► QUEUED ──► RUNNING ──► SUCCESS
 *                                ├──► FAILED
 *                                ├──► CANCELLED
 *                                └──► TIMED_OUT
 * FAILED ──► PENDING (retry)
 * TIMED_OUT ──► PENDING (retry)
 * </pre>
 */
@SuppressWarnings("java:S125") // JML contract annotations are intentional, not dead code
public enum ExecutionStatus {
    PENDING,
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED,
    SKIPPED,
    TIMED_OUT;

    /*@ ensures \result != null; @*/
    public /*@ pure @*/ Set<ExecutionStatus> validTargets() {
        return switch (this) {
            case PENDING -> Set.of(QUEUED, CANCELLED);
            case QUEUED -> Set.of(RUNNING, CANCELLED);
            case RUNNING -> Set.of(SUCCESS, FAILED, CANCELLED, TIMED_OUT);
            case FAILED -> Set.of(PENDING); // retry
            case TIMED_OUT -> Set.of(PENDING); // retry
            case SUCCESS, CANCELLED, SKIPPED -> Set.of();
        };
    }

    /*@ requires target != null; @*/
    public /*@ pure @*/ boolean canTransitionTo(ExecutionStatus target) {
        return validTargets().contains(target);
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || this == SKIPPED || this == TIMED_OUT;
    }
}
