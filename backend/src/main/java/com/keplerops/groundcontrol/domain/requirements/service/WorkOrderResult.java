package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record WorkOrderResult(
        int totalRequirements,
        int totalUnblocked,
        int totalBlocked,
        int totalUnconstrained,
        List<WorkOrderWave> waves) {}
