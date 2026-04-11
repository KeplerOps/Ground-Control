package com.keplerops.groundcontrol.api.controlpacks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateControlPackOverrideRequest(
        @NotBlank @Size(max = 100) String fieldName, String overrideValue, @Size(max = 500) String reason) {}
