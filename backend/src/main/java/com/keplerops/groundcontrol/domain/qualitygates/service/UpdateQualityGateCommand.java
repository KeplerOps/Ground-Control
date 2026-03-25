package com.keplerops.groundcontrol.domain.qualitygates.service;

import com.keplerops.groundcontrol.domain.qualitygates.state.ComparisonOperator;
import com.keplerops.groundcontrol.domain.qualitygates.state.MetricType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.util.Optional;

public record UpdateQualityGateCommand(
        String name,
        Optional<String> description,
        MetricType metricType,
        Optional<String> metricParam,
        Optional<Status> scopeStatus,
        ComparisonOperator operator,
        Double threshold,
        Boolean enabled) {}
