package com.keplerops.groundcontrol.api.controlpacks;

import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record ControlPackEntryDefinitionRequest(
        @NotBlank @Size(max = 50) String uid,
        @NotBlank @Size(max = 200) String title,
        @NotNull ControlFunction controlFunction,
        String description,
        String objective,
        @Size(max = 200) String owner,
        String implementationScope,
        Map<String, Object> methodologyFactors,
        Map<String, Object> effectiveness,
        @Size(max = 100) String category,
        @Size(max = 200) String source,
        String implementationGuidance,
        List<Map<String, Object>> expectedEvidence,
        List<Map<String, Object>> frameworkMappings) {}
