package com.keplerops.groundcontrol.api.requirements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloneRequirementRequest(@NotBlank @Size(max = 50) String newUid, boolean copyRelations) {}
