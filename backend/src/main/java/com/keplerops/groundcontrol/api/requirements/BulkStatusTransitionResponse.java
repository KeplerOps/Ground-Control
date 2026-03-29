package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.service.BulkFailureDetail;
import com.keplerops.groundcontrol.domain.requirements.service.BulkTransitionResult;
import java.util.List;

public record BulkStatusTransitionResponse(
        List<RequirementResponse> succeeded,
        List<BulkFailureDetail> failed,
        int totalRequested,
        int totalSucceeded,
        int totalFailed) {

    public static BulkStatusTransitionResponse from(BulkTransitionResult result, int totalRequested) {
        var succeeded =
                result.succeeded().stream().map(RequirementResponse::from).toList();
        return new BulkStatusTransitionResponse(
                succeeded,
                result.failed(),
                totalRequested,
                succeeded.size(),
                result.failed().size());
    }
}
