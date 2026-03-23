package com.keplerops.groundcontrol.api.baselines;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BaselineRequest(@NotBlank @Size(max = 100) String name, @Size(max = 5000) String description) {}
