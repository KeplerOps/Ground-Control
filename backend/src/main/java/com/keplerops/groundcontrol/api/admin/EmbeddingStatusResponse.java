package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingStatus;
import java.time.Instant;
import java.util.UUID;

public record EmbeddingStatusResponse(
        UUID requirementId,
        boolean hasEmbedding,
        boolean isStale,
        boolean modelMismatch,
        String currentModelId,
        String embeddingModelId,
        Instant embeddedAt) {

    public static EmbeddingStatusResponse from(EmbeddingStatus status) {
        return new EmbeddingStatusResponse(
                status.requirementId(),
                status.hasEmbedding(),
                status.isStale(),
                status.modelMismatch(),
                status.currentModelId(),
                status.embeddingModelId(),
                status.embeddedAt());
    }
}
