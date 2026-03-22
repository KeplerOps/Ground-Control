package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.UUID;

public record EmbeddingResult(UUID requirementId, String status, String modelId, String contentHash) {}
