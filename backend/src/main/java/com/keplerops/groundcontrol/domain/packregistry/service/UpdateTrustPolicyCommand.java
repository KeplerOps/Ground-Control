package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicyRule;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import java.util.List;

public record UpdateTrustPolicyCommand(
        String name,
        String description,
        TrustOutcome defaultOutcome,
        List<TrustPolicyRule> rules,
        Integer priority,
        Boolean enabled) {}
