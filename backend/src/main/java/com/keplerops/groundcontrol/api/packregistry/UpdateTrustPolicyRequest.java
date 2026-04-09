package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record UpdateTrustPolicyRequest(
        @Size(max = 200) String name,
        String description,
        TrustOutcome defaultOutcome,
        List<Map<String, Object>> rules,
        int priority,
        boolean enabled) {}
