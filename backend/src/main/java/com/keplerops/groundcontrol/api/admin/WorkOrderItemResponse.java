package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.WorkOrderItem;
import java.util.List;
import java.util.UUID;

public record WorkOrderItemResponse(
        UUID id,
        String uid,
        String title,
        String status,
        String priority,
        Integer wave,
        int order,
        String blockingStatus,
        List<String> blockedBy) {

    public static WorkOrderItemResponse from(WorkOrderItem item) {
        return new WorkOrderItemResponse(
                item.id(),
                item.uid(),
                item.title(),
                item.status(),
                item.priority(),
                item.wave(),
                item.order(),
                item.blockingStatus().name(),
                item.blockedBy());
    }
}
