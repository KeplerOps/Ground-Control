package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.WorkOrderResult;
import java.util.List;

public record WorkOrderResponse(
        int totalRequirements,
        int totalUnblocked,
        int totalBlocked,
        int totalUnconstrained,
        List<WorkOrderWaveResponse> waves) {

    public static WorkOrderResponse from(WorkOrderResult r) {
        return new WorkOrderResponse(
                r.totalRequirements(),
                r.totalUnblocked(),
                r.totalBlocked(),
                r.totalUnconstrained(),
                r.waves().stream().map(WorkOrderWaveResponse::from).toList());
    }
}
