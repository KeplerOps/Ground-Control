package com.keplerops.groundcontrol.api.qualitygates;

import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateEvaluationResult;
import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QualityGateEvaluationResponse(
        String projectIdentifier,
        Instant timestamp,
        boolean passed,
        int totalGates,
        int passedCount,
        int failedCount,
        List<GateResultResponse> gates) {

    public record GateResultResponse(
            UUID gateId,
            String gateName,
            String metricType,
            String metricParam,
            String scopeStatus,
            String operator,
            double threshold,
            double actualValue,
            boolean passed) {

        public static GateResultResponse from(QualityGateResult r) {
            return new GateResultResponse(
                    r.gateId(),
                    r.gateName(),
                    r.metricType(),
                    r.metricParam(),
                    r.scopeStatus(),
                    r.operator(),
                    r.threshold(),
                    r.actualValue(),
                    r.passed());
        }
    }

    public static QualityGateEvaluationResponse from(QualityGateEvaluationResult result) {
        return new QualityGateEvaluationResponse(
                result.projectIdentifier(),
                result.timestamp(),
                result.passed(),
                result.totalGates(),
                result.passedCount(),
                result.failedCount(),
                result.gates().stream().map(GateResultResponse::from).toList());
    }
}
