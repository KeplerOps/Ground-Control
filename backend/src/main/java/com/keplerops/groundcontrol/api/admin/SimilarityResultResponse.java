package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.SimilarityResult;
import java.util.List;

public record SimilarityResultResponse(
        int totalRequirements,
        int embeddedCount,
        int pairsAnalyzed,
        double threshold,
        List<SimilarityPairResponse> pairs) {

    public static SimilarityResultResponse from(SimilarityResult result) {
        return new SimilarityResultResponse(
                result.totalRequirements(),
                result.embeddedCount(),
                result.pairsAnalyzed(),
                result.threshold(),
                result.pairs().stream().map(SimilarityPairResponse::from).toList());
    }
}
