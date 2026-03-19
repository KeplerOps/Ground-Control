package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;

public record ConsistencyViolation(RequirementRelation relation, String violationType) {}
