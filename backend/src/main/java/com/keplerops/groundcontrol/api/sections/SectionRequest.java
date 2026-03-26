package com.keplerops.groundcontrol.api.sections;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SectionRequest(
        UUID parentId, @NotBlank @Size(max = 200) String title, String description, Integer sortOrder) {}
