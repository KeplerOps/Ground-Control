package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicyRule;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import java.util.List;
import java.util.UUID;

public record CreateTrustPolicyCommand(
        UUID projectId,
        String name,
        String description,
        TrustOutcome defaultOutcome,
        List<TrustPolicyRule> rules,
        int priority,
        boolean enabled) {}
