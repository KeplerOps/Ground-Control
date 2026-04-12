package com.keplerops.groundcontrol.api.threatmodels;

import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModelLink;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkType;
import java.time.Instant;
import java.util.UUID;

public record ThreatModelLinkResponse(
        UUID id,
        UUID threatModelId,
        ThreatModelLinkTargetType targetType,
        UUID targetEntityId,
        String targetIdentifier,
        ThreatModelLinkType linkType,
        String targetUrl,
        String targetTitle,
        Instant createdAt,
        Instant updatedAt) {

    public static ThreatModelLinkResponse from(ThreatModelLink link) {
        return new ThreatModelLinkResponse(
                link.getId(),
                link.getThreatModel().getId(),
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
