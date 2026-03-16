package com.keplerops.groundcontrol.api.projects;

import com.keplerops.groundcontrol.domain.projects.model.Project;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id, String identifier, String name, String description, Instant createdAt, Instant updatedAt) {

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(), p.getIdentifier(), p.getName(), p.getDescription(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
