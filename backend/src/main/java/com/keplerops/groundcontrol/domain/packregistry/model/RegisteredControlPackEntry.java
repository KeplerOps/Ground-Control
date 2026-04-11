package com.keplerops.groundcontrol.domain.packregistry.model;

import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import java.util.List;
import java.util.Map;

public record RegisteredControlPackEntry(
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
        String source,
        String implementationGuidance,
        List<Map<String, Object>> expectedEvidence,
        List<Map<String, Object>> frameworkMappings) {}
