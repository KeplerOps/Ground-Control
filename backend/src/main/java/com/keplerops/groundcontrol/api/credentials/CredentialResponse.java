package com.keplerops.groundcontrol.api.credentials;

import com.keplerops.groundcontrol.domain.credentials.model.Credential;
import java.time.Instant;
import java.util.UUID;

public record CredentialResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String credentialType,
        Instant createdAt,
        Instant updatedAt) {

    public static CredentialResponse from(Credential c) {
        return new CredentialResponse(
                c.getId(), c.getWorkspace().getId(), c.getName(), c.getCredentialType(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
