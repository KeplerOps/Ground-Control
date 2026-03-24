package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import java.time.Instant;

public record RelationRevision(
        int revisionNumber,
        Instant timestamp,
        String revisionType,
        String actor,
        String reason,
        RequirementRelation entity) {}
