package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.BatchEmbeddingResult;
import java.util.List;

public record BatchEmbeddingResultResponse(
        int total, int embedded, int skipped, int failed, String modelId, List<String> errors) {

    public static BatchEmbeddingResultResponse from(BatchEmbeddingResult result) {
        return new BatchEmbeddingResultResponse(
                result.total(),
                result.embedded(),
                result.skipped(),
                result.failed(),
                result.modelId(),
                result.errors());
    }
}
