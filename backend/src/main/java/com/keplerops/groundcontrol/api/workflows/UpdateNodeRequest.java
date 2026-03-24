package com.keplerops.groundcontrol.api.workflows;

import com.keplerops.groundcontrol.domain.workflows.state.NodeType;

public record UpdateNodeRequest(
        String name,
        String label,
        NodeType nodeType,
        String config,
        Integer positionX,
        Integer positionY,
        Integer timeoutSeconds,
        String retryPolicy) {}
