package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record BatchEmbeddingResult(
        int total, int embedded, int skipped, int failed, String modelId, List<String> errors) {}
