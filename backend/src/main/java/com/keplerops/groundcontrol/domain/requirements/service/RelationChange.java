package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.Map;
import java.util.UUID;

/** A single relation added, removed, or modified between two revisions. */
public record RelationChange(
        UUID relationId, String changeType, Map<String, Object> snapshot, Map<String, FieldChange> fieldChanges) {}
