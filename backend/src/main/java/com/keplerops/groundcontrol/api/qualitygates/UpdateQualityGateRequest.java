package com.keplerops.groundcontrol.api.qualitygates;

import com.keplerops.groundcontrol.domain.qualitygates.state.ComparisonOperator;
import com.keplerops.groundcontrol.domain.qualitygates.state.MetricType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import jakarta.validation.constraints.Size;
import java.util.Optional;

public record UpdateQualityGateRequest(
        @Size(max = 100) String name,
        Optional<String> description,
        MetricType metricType,
        Optional<String> metricParam,
        Optional<Status> scopeStatus,
        ComparisonOperator operator,
        Double threshold,
        Boolean enabled) {}
