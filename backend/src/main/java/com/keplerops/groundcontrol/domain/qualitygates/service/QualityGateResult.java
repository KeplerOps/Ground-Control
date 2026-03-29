package com.keplerops.groundcontrol.domain.qualitygates.service;

import java.util.UUID;

public record QualityGateResult(
        UUID gateId,
        String gateName,
        String metricType,
        String metricParam,
        String scopeStatus,
        String operator,
        double threshold,
        double actualValue,
        boolean passed) {}
