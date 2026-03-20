package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.state.RelationType;

public record ReqifRelation(String sourceIdentifier, String targetIdentifier, RelationType relationType) {}
