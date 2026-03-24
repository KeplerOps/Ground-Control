package com.keplerops.groundcontrol.api.workspaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceRequest(
        @NotBlank @Size(max = 50) String identifier,
        @NotBlank String name,
        String description) {}
