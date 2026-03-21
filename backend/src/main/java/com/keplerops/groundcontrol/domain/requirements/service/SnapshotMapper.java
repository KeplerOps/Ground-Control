package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.RequirementRelation;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Converts audited entities to flat snapshot maps for timeline diff computation. */
public final class SnapshotMapper {

    private SnapshotMapper() {}

    public static Map<String, Object> fromRequirement(Requirement r) {
        var map = new LinkedHashMap<String, Object>();
        map.put("uid", r.getUid());
        map.put("title", r.getTitle());
        map.put("statement", r.getStatement());
        map.put("rationale", r.getRationale());
        map.put(
                "requirementType",
                r.getRequirementType() != null ? r.getRequirementType().name() : null);
        map.put("priority", r.getPriority() != null ? r.getPriority().name() : null);
        map.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        map.put("wave", r.getWave());
        return map;
    }

    public static Map<String, Object> fromRelation(RequirementRelation r) {
        var map = new LinkedHashMap<String, Object>();
        map.put("sourceId", r.getSource() != null ? r.getSource().getId().toString() : null);
        map.put("targetId", r.getTarget() != null ? r.getTarget().getId().toString() : null);
        map.put(
                "relationType",
                r.getRelationType() != null ? r.getRelationType().name() : null);
        map.put("description", r.getDescription());
        return map;
    }

    public static Map<String, Object> fromTraceabilityLink(TraceabilityLink t) {
        var map = new LinkedHashMap<String, Object>();
        map.put(
                "artifactType",
                t.getArtifactType() != null ? t.getArtifactType().name() : null);
        map.put("artifactIdentifier", t.getArtifactIdentifier());
        map.put("artifactUrl", t.getArtifactUrl());
        map.put("artifactTitle", t.getArtifactTitle());
        map.put("linkType", t.getLinkType() != null ? t.getLinkType().name() : null);
        map.put("syncStatus", t.getSyncStatus() != null ? t.getSyncStatus().name() : null);
        return map;
    }

    public static Map<String, FieldChange> computeDiff(Map<String, Object> previous, Map<String, Object> current) {
        var diff = new LinkedHashMap<String, FieldChange>();
        for (var entry : current.entrySet()) {
            var oldVal = previous.get(entry.getKey());
            if (!Objects.equals(oldVal, entry.getValue())) {
                diff.put(entry.getKey(), new FieldChange(oldVal, entry.getValue()));
            }
        }
        return diff;
    }
}
