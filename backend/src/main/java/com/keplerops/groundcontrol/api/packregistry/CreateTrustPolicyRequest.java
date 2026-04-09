package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record CreateTrustPolicyRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull TrustOutcome defaultOutcome,
        List<Map<String, Object>> rules,
        int priority,
        boolean enabled) {}
