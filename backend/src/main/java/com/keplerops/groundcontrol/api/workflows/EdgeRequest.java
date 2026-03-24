package com.keplerops.groundcontrol.api.workflows;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EdgeRequest(
        @NotNull UUID sourceNodeId,
        @NotNull UUID targetNodeId,
        String conditionExpr,
        String label) {}
