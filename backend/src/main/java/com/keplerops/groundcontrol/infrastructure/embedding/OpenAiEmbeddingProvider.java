package com.keplerops.groundcontrol.infrastructure.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingProvider;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "groundcontrol.embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingProvider.class);

    private final EmbeddingProperties properties;
    private final RestClient restClient;

    public OpenAiEmbeddingProvider(EmbeddingProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.apiUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .build();
    }

    @Override
    public boolean isAvailable() {
        return properties.apiKey() != null && !properties.apiKey().isBlank();
    }

    @Override
    public String getModelId() {
        return properties.model();
    }

    @Override
    public int getDimensions() {
        return properties.dimensions();
    }

    @Override
    public float[] embed(String text) {
        var results = embedBatch(List.of(text));
        return results.getFirst();
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        log.debug("openai_embedding_request: count={} model={}", texts.size(), properties.model());

        var requestBody = Map.of(
                "input", texts,
                "model", properties.model(),
                "dimensions", properties.dimensions());

        var response = restClient
                .post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null) {
            throw new IllegalStateException("Empty response from OpenAI embeddings API");
        }

        var sorted = response.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingData::index))
                .toList();

        log.debug(
                "openai_embedding_response: count={} model={} usage={}",
                sorted.size(),
                response.model(),
                response.usage());

        return sorted.stream().map(d -> toFloatArray(d.embedding())).toList();
    }

    private static float[] toFloatArray(List<Double> doubles) {
        var floats = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            floats[i] = doubles.get(i).floatValue();
        }
        return floats;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingResponse(List<EmbeddingData> data, String model, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingData(int index, List<Double> embedding) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("prompt_tokens") int promptTokens, @JsonProperty("total_tokens") int totalTokens) {}
}
