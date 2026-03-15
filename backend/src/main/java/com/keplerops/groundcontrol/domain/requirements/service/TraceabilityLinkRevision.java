package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import java.time.Instant;

public record TraceabilityLinkRevision(
        int revisionNumber, Instant timestamp, String revisionType, String actor, TraceabilityLink entity) {}
