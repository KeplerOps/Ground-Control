package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.packregistry.model.TrustPolicy;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TrustPolicyResponse(
        UUID id,
        String projectIdentifier,
        String name,
        String description,
        TrustOutcome defaultOutcome,
        List<Map<String, Object>> rules,
        int priority,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static TrustPolicyResponse from(TrustPolicy policy) {
        return new TrustPolicyResponse(
                policy.getId(),
                policy.getProject().getIdentifier(),
                policy.getName(),
                policy.getDescription(),
                policy.getDefaultOutcome(),
                policy.getRules(),
                policy.getPriority(),
                policy.isEnabled(),
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }
}
