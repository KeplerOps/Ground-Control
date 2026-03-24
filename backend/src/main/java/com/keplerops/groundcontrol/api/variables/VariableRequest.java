package com.keplerops.groundcontrol.api.variables;

import jakarta.validation.constraints.NotBlank;

public record VariableRequest(
        @NotBlank String key,
        String value,
        String description,
        Boolean secret) {}
