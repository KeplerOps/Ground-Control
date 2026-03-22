package com.keplerops.groundcontrol.infrastructure.embedding;

import com.keplerops.groundcontrol.domain.requirements.service.EmbeddingProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(OpenAiEmbeddingProvider.class)
public class NoOpEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmbeddingProvider.class);

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getModelId() {
        return "none";
    }

    @Override
    public int getDimensions() {
        return 0;
    }

    @Override
    public float[] embed(String text) {
        log.debug("embedding_skipped: reason=no_provider_configured");
        return new float[0];
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        log.debug("batch_embedding_skipped: reason=no_provider_configured count={}", texts.size());
        return texts.stream().map(t -> new float[0]).toList();
    }
}
