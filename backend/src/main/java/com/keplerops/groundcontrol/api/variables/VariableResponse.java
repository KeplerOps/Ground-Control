package com.keplerops.groundcontrol.api.variables;

import com.keplerops.groundcontrol.domain.variables.model.Variable;
import java.time.Instant;
import java.util.UUID;

public record VariableResponse(
        UUID id,
        UUID workspaceId,
        String key,
        String value,
        String description,
        boolean secret,
        Instant createdAt,
        Instant updatedAt) {

    public static VariableResponse from(Variable v) {
        return new VariableResponse(
                v.getId(), v.getWorkspace().getId(), v.getKey(),
                v.isSecret() ? "***" : v.getValue(),
                v.getDescription(), v.isSecret(), v.getCreatedAt(), v.getUpdatedAt());
    }
}
