package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.service.FieldChange;
import com.keplerops.groundcontrol.domain.requirements.service.RelationChange;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementVersionDiff;
import com.keplerops.groundcontrol.domain.requirements.service.TraceabilityLinkChange;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RequirementVersionDiffResponse(
        UUID requirementId,
        int fromRevision,
        int toRevision,
        Map<String, FieldChange> fieldChanges,
        List<RelationChange> relationChanges,
        List<TraceabilityLinkChange> traceabilityLinkChanges) {

    public static RequirementVersionDiffResponse from(RequirementVersionDiff diff) {
        return new RequirementVersionDiffResponse(
                diff.requirementId(),
                diff.fromRevision(),
                diff.toRevision(),
                diff.fieldChanges(),
                diff.relationChanges(),
                diff.traceabilityLinkChanges());
    }
}
