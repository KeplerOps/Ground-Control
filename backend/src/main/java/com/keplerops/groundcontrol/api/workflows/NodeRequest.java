package com.keplerops.groundcontrol.api.workflows;

import com.keplerops.groundcontrol.domain.workflows.state.NodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NodeRequest(
        @NotBlank String name,
        @NotNull NodeType nodeType,
        String label,
        String config,
        Integer positionX,
        Integer positionY,
        Integer timeoutSeconds,
        String retryPolicy) {}
