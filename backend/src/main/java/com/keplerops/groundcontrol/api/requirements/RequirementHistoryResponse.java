package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.service.RequirementRevision;
import java.time.Instant;

public record RequirementHistoryResponse(
        int revisionNumber, String revisionType, Instant timestamp, String actor, RequirementResponse snapshot) {

    public static RequirementHistoryResponse from(RequirementRevision revision) {
        return new RequirementHistoryResponse(
                revision.revisionNumber(),
                revision.revisionType(),
                revision.timestamp(),
                revision.actor(),
                RequirementResponse.from(revision.entity()));
    }
}
