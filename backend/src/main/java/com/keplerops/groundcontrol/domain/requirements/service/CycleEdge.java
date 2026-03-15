package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.state.RelationType;

public record CycleEdge(String sourceUid, String targetUid, RelationType relationType) {}
