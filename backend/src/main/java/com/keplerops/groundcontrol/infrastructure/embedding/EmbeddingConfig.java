package com.keplerops.groundcontrol.infrastructure.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Value("${groundcontrol.embedding.provider:none}")
    private String provider;

    @Value("${groundcontrol.embedding.api-key:}")
    private String apiKey;

    @Value("${groundcontrol.embedding.api-url:https://api.openai.com/v1}")
    private String apiUrl;

    @Value("${groundcontrol.embedding.model:text-embedding-3-small}")
    private String model;

    @Value("${groundcontrol.embedding.dimensions:1536}")
    private int dimensions;

    @Value("${groundcontrol.embedding.batch-size:100}")
    private int batchSize;

    @Bean
    EmbeddingProperties embeddingProperties() {
        return new EmbeddingProperties(provider, apiKey, apiUrl, model, dimensions, batchSize);
    }
}
