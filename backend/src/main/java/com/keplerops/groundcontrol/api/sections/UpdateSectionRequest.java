package com.keplerops.groundcontrol.api.sections;

import jakarta.validation.constraints.Size;

public record UpdateSectionRequest(@Size(max = 200) String title, String description, Integer sortOrder) {}
