package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.infrastructure.embedding.NoOpEmbeddingProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoOpEmbeddingProviderTest {

    private final NoOpEmbeddingProvider provider = new NoOpEmbeddingProvider();

    @Test
    void isAvailable_returnsFalse() {
        assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    void getModelId_returnsNone() {
        assertThat(provider.getModelId()).isEqualTo("none");
    }

    @Test
    void getDimensions_returnsZero() {
        assertThat(provider.getDimensions()).isZero();
    }

    @Test
    void embed_returnsEmptyArray() {
        var result = provider.embed("some text");
        assertThat(result).isEmpty();
    }

    @Test
    void embedBatch_returnsEmptyArraysForEachInput() {
        var result = provider.embedBatch(List.of("text1", "text2", "text3"));
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(arr -> arr.length == 0);
    }
}
