package com.keplerops.groundcontrol.domain.assets.service;

import java.util.List;

public record AssetCycleResult(List<String> memberUids, List<AssetCycleEdge> edges) {}
