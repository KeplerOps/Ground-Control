package com.keplerops.groundcontrol.api.findings;

import com.keplerops.groundcontrol.domain.findings.model.FindingLink;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkType;
import java.time.Instant;
import java.util.UUID;

public record FindingLinkResponse(
        UUID id,
        UUID findingId,
        FindingLinkTargetType targetType,
        UUID targetEntityId,
        String targetIdentifier,
        FindingLinkType linkType,
        String targetUrl,
        String targetTitle,
        Instant createdAt,
        Instant updatedAt) {

    public static FindingLinkResponse from(FindingLink link, UUID findingId) {
        return new FindingLinkResponse(
                link.getId(),
                findingId,
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
