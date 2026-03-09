package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RequirementType;

public record CreateRequirementCommand(
        String uid,
        String title,
        String statement,
        String rationale,
        RequirementType requirementType,
        Priority priority,
        Integer wave) {}
