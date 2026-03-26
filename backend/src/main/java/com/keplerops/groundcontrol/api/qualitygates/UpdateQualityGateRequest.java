package com.keplerops.groundcontrol.api.qualitygates;

import com.keplerops.groundcontrol.domain.qualitygates.state.ComparisonOperator;
import com.keplerops.groundcontrol.domain.qualitygates.state.MetricType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import jakarta.validation.constraints.Size;

public record UpdateQualityGateRequest(
        @Size(max = 100) String name,
        String description,
        MetricType metricType,
        String metricParam,
        Status scopeStatus,
        ComparisonOperator operator,
        Double threshold,
        Boolean enabled) {}
