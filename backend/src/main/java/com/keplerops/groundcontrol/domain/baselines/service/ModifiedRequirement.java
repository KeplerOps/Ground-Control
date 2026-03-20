package com.keplerops.groundcontrol.domain.baselines.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.util.UUID;

public record ModifiedRequirement(UUID requirementId, String uid, Requirement before, Requirement after) {}
