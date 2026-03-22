package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.WorkOrderWave;
import java.util.List;

public record WorkOrderWaveResponse(
        Integer wave, int total, int unblocked, int blocked, int unconstrained, List<WorkOrderItemResponse> items) {

    public static WorkOrderWaveResponse from(WorkOrderWave w) {
        return new WorkOrderWaveResponse(
                w.wave(),
                w.total(),
                w.unblocked(),
                w.blocked(),
                w.unconstrained(),
                w.items().stream().map(WorkOrderItemResponse::from).toList());
    }
}
