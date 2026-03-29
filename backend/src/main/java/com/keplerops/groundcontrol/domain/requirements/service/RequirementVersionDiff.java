package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Structured diff between two revisions of a single requirement. */
public record RequirementVersionDiff(
        UUID requirementId,
        int fromRevision,
        int toRevision,
        Map<String, FieldChange> fieldChanges,
        List<RelationChange> relationChanges,
        List<TraceabilityLinkChange> traceabilityLinkChanges) {}
