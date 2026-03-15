package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record CycleResult(List<String> members, List<CycleEdge> edges) {}
