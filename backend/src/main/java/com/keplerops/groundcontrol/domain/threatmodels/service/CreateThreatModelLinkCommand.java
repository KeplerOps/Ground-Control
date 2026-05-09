package com.keplerops.groundcontrol.domain.threatmodels.service;

import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkType;
import java.util.UUID;

public record CreateThreatModelLinkCommand(
        ThreatModelLinkTargetType targetType,
        UUID targetEntityId,
        String targetIdentifier,
        ThreatModelLinkType linkType,
        String targetUrl,
        String targetTitle) {}
