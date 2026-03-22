package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.infrastructure.embedding.EmbeddingProperties;
import com.keplerops.groundcontrol.infrastructure.embedding.OpenAiEmbeddingProvider;
import org.junit.jupiter.api.Test;

class OpenAiEmbeddingProviderTest {

    @Test
    void isAvailable_returnsTrueWhenApiKeySet() {
        var props =
                new EmbeddingProperties("openai", "sk-test-key", "https://api.openai.com/v1", "model", 1536, 100, 0.85);
        var provider = new OpenAiEmbeddingProvider(props);
        assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    void isAvailable_returnsFalseWhenApiKeyBlank() {
        var props = new EmbeddingProperties("openai", "", "https://api.openai.com/v1", "model", 1536, 100, 0.85);
        var provider = new OpenAiEmbeddingProvider(props);
        assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_returnsFalseWhenApiKeyNull() {
        var props = new EmbeddingProperties("openai", null, "https://api.openai.com/v1", "model", 1536, 100, 0.85);
        var provider = new OpenAiEmbeddingProvider(props);
        assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    void getModelId_returnsConfiguredModel() {
        var props = new EmbeddingProperties(
                "openai", "sk-test", "https://api.openai.com/v1", "text-embedding-3-small", 1536, 100, 0.85);
        var provider = new OpenAiEmbeddingProvider(props);
        assertThat(provider.getModelId()).isEqualTo("text-embedding-3-small");
    }

    @Test
    void getDimensions_returnsConfiguredDimensions() {
        var props = new EmbeddingProperties("openai", "sk-test", "https://api.openai.com/v1", "model", 768, 100, 0.85);
        var provider = new OpenAiEmbeddingProvider(props);
        assertThat(provider.getDimensions()).isEqualTo(768);
    }
}
