package com.keplerops.groundcontrol.domain.requirements.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** A single entry in the unified audit timeline for a requirement. */
public record TimelineEntry(
        int revisionNumber,
        String revisionType,
        Instant timestamp,
        String actor,
        String changeCategory,
        UUID entityId,
        Map<String, Object> snapshot,
        Map<String, FieldChange> changes) {}
