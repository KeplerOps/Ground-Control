package com.keplerops.groundcontrol.domain.audits.service;

import com.keplerops.groundcontrol.domain.audits.state.AuditLinkTargetType;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkType;
import java.util.UUID;

public record CreateAuditLinkCommand(
        AuditLinkTargetType targetType,
        UUID targetEntityId,
        String targetIdentifier,
        AuditLinkType linkType,
        String targetUrl,
        String targetTitle) {}
