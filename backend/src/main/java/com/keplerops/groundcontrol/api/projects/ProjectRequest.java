package com.keplerops.groundcontrol.api.projects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "[a-z0-9-]+") String identifier,
        @NotBlank @Size(max = 255) String name,
        String description) {}
