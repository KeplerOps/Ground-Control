package com.keplerops.groundcontrol.domain.graph.service;

import java.util.List;

public record GraphPathResult(List<String> nodeIds, List<String> edgeTypes) {}
