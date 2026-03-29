package com.keplerops.groundcontrol.api.qualitygates;

import com.keplerops.groundcontrol.domain.qualitygates.model.QualityGate;
import java.time.Instant;
import java.util.UUID;

public record QualityGateResponse(
        UUID id,
        String projectIdentifier,
        String name,
        String description,
        String metricType,
        String metricParam,
        String scopeStatus,
        String operator,
        double threshold,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static QualityGateResponse from(QualityGate gate) {
        return new QualityGateResponse(
                gate.getId(),
                gate.getProject().getIdentifier(),
                gate.getName(),
                gate.getDescription(),
                gate.getMetricType().name(),
                gate.getMetricParam(),
                gate.getScopeStatus() != null ? gate.getScopeStatus().name() : null,
                gate.getOperator().name(),
                gate.getThreshold(),
                gate.isEnabled(),
                gate.getCreatedAt(),
                gate.getUpdatedAt());
    }
}
