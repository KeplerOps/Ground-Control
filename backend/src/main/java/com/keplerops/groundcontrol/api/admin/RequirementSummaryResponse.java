package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import java.util.UUID;

public record RequirementSummaryResponse(UUID id, String uid, String title, String status, Integer wave) {

    public static RequirementSummaryResponse from(Requirement r) {
        return new RequirementSummaryResponse(
                r.getId(), r.getUid(), r.getTitle(), r.getStatus().name(), r.getWave());
    }
}
