package com.keplerops.groundcontrol.domain.controlpacks.service;

import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import java.util.List;
import java.util.Map;

public record ControlPackEntryDefinition(
        String uid,
        String title,
        String description,
        String objective,
        ControlFunction controlFunction,
        String owner,
        String implementationScope,
        Map<String, Object> methodologyFactors,
        Map<String, Object> effectiveness,
        String category,
        String source,
        String implementationGuidance,
        List<Map<String, Object>> expectedEvidence,
        List<Map<String, Object>> frameworkMappings) {}
