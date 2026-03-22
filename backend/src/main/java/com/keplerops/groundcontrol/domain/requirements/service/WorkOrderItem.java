package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;
import java.util.UUID;

public record WorkOrderItem(
        UUID id,
        String uid,
        String title,
        String status,
        String priority,
        Integer wave,
        int order,
        BlockingStatus blockingStatus,
        List<String> blockedBy) {}
