package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateTrustPolicyCommand(
        UUID projectId,
        String name,
        String description,
        TrustOutcome defaultOutcome,
        List<Map<String, Object>> rules,
        int priority,
        boolean enabled) {}
