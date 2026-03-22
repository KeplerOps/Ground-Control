package com.keplerops.groundcontrol.domain.requirements.service;

import java.time.Instant;
import java.util.UUID;

public record EmbeddingStatus(
        UUID requirementId,
        boolean hasEmbedding,
        boolean isStale,
        boolean modelMismatch,
        String currentModelId,
        String embeddingModelId,
        Instant embeddedAt) {}
