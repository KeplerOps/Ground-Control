package com.keplerops.groundcontrol.domain.controls.service;

import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import java.util.Map;
import java.util.UUID;

public record CreateControlCommand(
        UUID projectId,
        String uid,
        String title,
        ControlFunction controlFunction,
        String description,
        String objective,
        String owner,
        String implementationScope,
        Map<String, Object> methodologyFactors,
        Map<String, Object> effectiveness,
        String category,
        String source) {}
