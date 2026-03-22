package com.keplerops.groundcontrol.api.baselines;

import com.keplerops.groundcontrol.api.requirements.RequirementResponse;
import com.keplerops.groundcontrol.domain.baselines.service.ModifiedRequirement;
import java.util.UUID;

public record ModifiedRequirementResponse(
        UUID requirementId, String uid, RequirementResponse before, RequirementResponse after) {

    public static ModifiedRequirementResponse from(ModifiedRequirement m) {
        return new ModifiedRequirementResponse(
                m.requirementId(), m.uid(), RequirementResponse.from(m.before()), RequirementResponse.from(m.after()));
    }
}
