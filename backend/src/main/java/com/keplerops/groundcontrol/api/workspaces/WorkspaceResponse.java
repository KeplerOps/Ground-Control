package com.keplerops.groundcontrol.api.workspaces;

import com.keplerops.groundcontrol.domain.workspaces.model.Workspace;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String identifier,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt) {

    public static WorkspaceResponse from(Workspace w) {
        return new WorkspaceResponse(
                w.getId(), w.getIdentifier(), w.getName(), w.getDescription(),
                w.getCreatedAt(), w.getUpdatedAt());
    }
}
