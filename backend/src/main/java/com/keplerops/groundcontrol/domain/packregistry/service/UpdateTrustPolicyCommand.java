package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import java.util.List;
import java.util.Map;

public record UpdateTrustPolicyCommand(
        String name,
        String description,
        TrustOutcome defaultOutcome,
        List<Map<String, Object>> rules,
        int priority,
        boolean enabled) {}
