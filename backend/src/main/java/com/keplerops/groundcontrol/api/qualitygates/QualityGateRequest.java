package com.keplerops.groundcontrol.api.qualitygates;

import com.keplerops.groundcontrol.domain.qualitygates.state.ComparisonOperator;
import com.keplerops.groundcontrol.domain.qualitygates.state.MetricType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QualityGateRequest(
        @NotBlank @Size(max = 100) String name,
        String description,
        @NotNull MetricType metricType,
        String metricParam,
        Status scopeStatus,
        @NotNull ComparisonOperator operator,
        @NotNull Double threshold) {}
