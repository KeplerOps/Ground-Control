package com.keplerops.groundcontrol.api.baselines;

import com.keplerops.groundcontrol.domain.baselines.model.Baseline;
import java.time.Instant;
import java.util.UUID;

public record BaselineResponse(
        UUID id,
        String projectIdentifier,
        String name,
        String description,
        int revisionNumber,
        Instant createdAt,
        String createdBy) {

    public static BaselineResponse from(Baseline b) {
        return new BaselineResponse(
                b.getId(),
                b.getProject() != null ? b.getProject().getIdentifier() : null,
                b.getName(),
                b.getDescription(),
                b.getRevisionNumber(),
                b.getCreatedAt(),
                b.getCreatedBy());
    }
}
