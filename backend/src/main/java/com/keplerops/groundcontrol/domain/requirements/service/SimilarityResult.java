package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record SimilarityResult(
        int totalRequirements, int embeddedCount, int pairsAnalyzed, double threshold, List<SimilarityPair> pairs) {}
