package com.keplerops.groundcontrol.domain.graph.model;

import java.util.List;

public record GraphProjection(List<GraphNode> nodes, List<GraphEdge> edges) {}
