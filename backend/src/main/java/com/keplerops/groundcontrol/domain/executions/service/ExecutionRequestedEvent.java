package com.keplerops.groundcontrol.domain.executions.service;

import java.util.UUID;

/**
 * Domain event published when a workflow execution is created and should be run.
 */
public record ExecutionRequestedEvent(UUID executionId) {}
