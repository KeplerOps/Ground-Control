package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record WorkOrderWave(
        Integer wave, int total, int unblocked, int blocked, int unconstrained, List<WorkOrderItem> items) {}
