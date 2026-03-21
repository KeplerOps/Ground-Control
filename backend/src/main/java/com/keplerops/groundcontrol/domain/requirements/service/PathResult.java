package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

/**
 * A single path between two requirements, containing both the requirement UIDs along the path and the relation type
 * labels for each edge. Edge {@code edgeLabels[i]} connects {@code nodeUids[i]} to {@code nodeUids[i+1]}.
 */
public record PathResult(List<String> nodeUids, List<String> edgeLabels) {}
