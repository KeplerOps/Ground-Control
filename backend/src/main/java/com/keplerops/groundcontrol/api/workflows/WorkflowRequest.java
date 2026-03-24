package com.keplerops.groundcontrol.api.workflows;

import jakarta.validation.constraints.NotBlank;

public record WorkflowRequest(
        @NotBlank String name,
        String description,
        String tags,
        Integer timeoutSeconds,
        Integer maxRetries,
        Integer retryBackoffMs) {}
