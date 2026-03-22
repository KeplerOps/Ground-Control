package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public interface EmbeddingProvider {

    boolean isAvailable();

    String getModelId();

    int getDimensions();

    float[] embed(String text);

    List<float[]> embedBatch(List<String> texts);
}
