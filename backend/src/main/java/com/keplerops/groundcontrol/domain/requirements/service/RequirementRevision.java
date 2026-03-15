package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.time.Instant;

public record RequirementRevision(
        int revisionNumber, Instant timestamp, String revisionType, String actor, Requirement entity) {}
