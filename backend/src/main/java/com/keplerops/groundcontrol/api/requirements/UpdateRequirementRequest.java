package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RequirementType;
import jakarta.validation.constraints.Size;

public record UpdateRequirementRequest(
        @Size(min = 1, max = 255) String title,
        @Size(min = 1, max = 50000) String statement,
        @Size(max = 50000) String rationale,
        RequirementType requirementType,
        Priority priority,
        Integer wave) {}
