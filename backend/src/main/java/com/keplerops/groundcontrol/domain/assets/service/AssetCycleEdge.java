package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;

public record AssetCycleEdge(String sourceUid, String targetUid, AssetRelationType relationType) {}
