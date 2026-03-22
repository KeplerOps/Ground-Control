package com.keplerops.groundcontrol.api.baselines;

import com.keplerops.groundcontrol.api.requirements.RequirementResponse;
import com.keplerops.groundcontrol.domain.baselines.service.BaselineComparison;
import java.util.List;
import java.util.UUID;

public record BaselineComparisonResponse(
        UUID baselineId,
        String baselineName,
        UUID otherBaselineId,
        String otherBaselineName,
        int addedCount,
        int removedCount,
        int modifiedCount,
        List<RequirementResponse> added,
        List<RequirementResponse> removed,
        List<ModifiedRequirementResponse> modified) {

    public static BaselineComparisonResponse from(BaselineComparison c) {
        var addedReqs = c.added().stream().map(RequirementResponse::from).toList();
        var removedReqs = c.removed().stream().map(RequirementResponse::from).toList();
        var modifiedReqs =
                c.modified().stream().map(ModifiedRequirementResponse::from).toList();
        return new BaselineComparisonResponse(
                c.baselineId(),
                c.baselineName(),
                c.otherBaselineId(),
                c.otherBaselineName(),
                addedReqs.size(),
                removedReqs.size(),
                modifiedReqs.size(),
                addedReqs,
                removedReqs,
                modifiedReqs);
    }
}
