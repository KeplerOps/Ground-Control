package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingResult;
import java.util.UUID;

public record EmbeddingResultResponse(UUID requirementId, String status, String modelId, String contentHash) {

    public static EmbeddingResultResponse from(EmbeddingResult result) {
        return new EmbeddingResultResponse(
                result.requirementId(), result.status(), result.modelId(), result.contentHash());
    }
}
