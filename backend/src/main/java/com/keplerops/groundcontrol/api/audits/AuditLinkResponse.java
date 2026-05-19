package com.keplerops.groundcontrol.api.audits;

import com.keplerops.groundcontrol.domain.audits.model.AuditLink;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkTargetType;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkType;
import java.time.Instant;
import java.util.UUID;

public record AuditLinkResponse(
        UUID id,
        UUID auditId,
        AuditLinkTargetType targetType,
        UUID targetEntityId,
        String targetIdentifier,
        AuditLinkType linkType,
        String targetUrl,
        String targetTitle,
        Instant createdAt,
        Instant updatedAt) {

    public static AuditLinkResponse from(AuditLink link, UUID auditId) {
        return new AuditLinkResponse(
                link.getId(),
                auditId,
                link.getTargetType(),
                link.getTargetEntityId(),
                link.getTargetIdentifier(),
                link.getLinkType(),
                link.getTargetUrl(),
                link.getTargetTitle(),
                link.getCreatedAt(),
                link.getUpdatedAt());
    }
}
