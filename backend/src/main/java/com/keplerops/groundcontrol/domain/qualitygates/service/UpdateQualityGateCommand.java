package com.keplerops.groundcontrol.domain.qualitygates.service;

import com.keplerops.groundcontrol.domain.qualitygates.state.ComparisonOperator;
import com.keplerops.groundcontrol.domain.qualitygates.state.MetricType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;

public record UpdateQualityGateCommand(
        String name,
        String description,
        MetricType metricType,
        String metricParam,
        Status scopeStatus,
        ComparisonOperator operator,
        Double threshold,
        Boolean enabled) {}
