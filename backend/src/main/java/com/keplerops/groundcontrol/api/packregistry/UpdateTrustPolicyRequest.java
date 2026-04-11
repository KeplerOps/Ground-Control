package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateTrustPolicyRequest(
        @Size(max = 200) String name,
        String description,
        TrustOutcome defaultOutcome,
        @Valid List<TrustPolicyRuleRequest> rules,
        Integer priority,
        Boolean enabled) {}
