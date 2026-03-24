package com.keplerops.groundcontrol.domain.workflows.state;

/**
 * Types of workflow nodes (tasks/steps).
 */
public enum NodeType {
    /** Execute a shell command. */
    SHELL,

    /** Make an HTTP request. */
    HTTP,

    /** Run a Docker container. */
    DOCKER,

    /** Execute a script (Python, Node.js, etc.). */
    SCRIPT,

    /** Conditional branching based on expression evaluation. */
    CONDITIONAL,

    /** Wait for a specified duration. */
    DELAY,

    /** Invoke another workflow as a sub-workflow. */
    SUB_WORKFLOW,

    /** Transform data using jq/JSONPath expressions. */
    TRANSFORM,

    /** Send a notification (email, Slack, webhook). */
    NOTIFICATION,

    /** No-op placeholder node. */
    NOOP
}
