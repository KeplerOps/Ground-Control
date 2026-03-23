package com.keplerops.groundcontrol.api.projects;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(@Size(min = 1, max = 255) String name, @Size(max = 5000) String description) {}
