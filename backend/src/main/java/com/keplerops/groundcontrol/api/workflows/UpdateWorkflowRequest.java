package com.keplerops.groundcontrol.api.workflows;

public record UpdateWorkflowRequest(
        String name,
        String description,
        String tags,
        Integer timeoutSeconds,
        Integer maxRetries,
        Integer retryBackoffMs) {}
