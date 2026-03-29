package com.keplerops.groundcontrol.domain.qualitygates.service;

import com.keplerops.groundcontrol.domain.qualitygates.state.ComparisonOperator;
import com.keplerops.groundcontrol.domain.qualitygates.state.MetricType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import java.util.UUID;

public record CreateQualityGateCommand(
        UUID projectId,
        String name,
        String description,
        MetricType metricType,
        String metricParam,
        Status scopeStatus,
        ComparisonOperator operator,
        double threshold) {}
