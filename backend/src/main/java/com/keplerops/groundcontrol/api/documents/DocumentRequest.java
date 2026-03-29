package com.keplerops.groundcontrol.api.documents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentRequest(
        @NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 50) String version, String description) {}
