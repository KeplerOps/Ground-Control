package com.keplerops.groundcontrol.infrastructure.embedding;

public record EmbeddingProperties(
        String provider,
        String apiKey,
        String apiUrl,
        String model,
        int dimensions,
        int batchSize,
        double similarityThreshold) {}
