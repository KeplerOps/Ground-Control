package com.keplerops.groundcontrol.domain.findings.service;

import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkType;
import java.util.UUID;

public record CreateFindingLinkCommand(
        FindingLinkTargetType targetType,
        UUID targetEntityId,
        String targetIdentifier,
        FindingLinkType linkType,
        String targetUrl,
        String targetTitle) {}
