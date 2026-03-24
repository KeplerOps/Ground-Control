package com.keplerops.groundcontrol.domain.workflows.state;

/**
 * Types of workflow triggers.
 */
public enum TriggerType {
    /** Manually triggered via API/GUI. */
    MANUAL,

    /** Triggered on a cron schedule. */
    CRON,

    /** Triggered by an incoming webhook. */
    WEBHOOK,

    /** Triggered by an internal event. */
    EVENT
}
