package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.Map;
import java.util.UUID;

/** A single traceability link added, removed, or modified between two revisions. */
public record TraceabilityLinkChange(
        UUID linkId, String changeType, Map<String, Object> snapshot, Map<String, FieldChange> fieldChanges) {}
