package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RequirementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequirementRequest(
        @NotBlank @Size(max = 50) String uid,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 50000) String statement,
        @Size(max = 50000) String rationale,
        RequirementType requirementType,
        Priority priority,
        Integer wave) {}
