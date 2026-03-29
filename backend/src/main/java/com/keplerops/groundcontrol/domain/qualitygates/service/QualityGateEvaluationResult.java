package com.keplerops.groundcontrol.domain.qualitygates.service;

import java.time.Instant;
import java.util.List;

public record QualityGateEvaluationResult(
        String projectIdentifier,
        Instant timestamp,
        boolean passed,
        int totalGates,
        int passedCount,
        int failedCount,
        List<QualityGateResult> gates) {}
