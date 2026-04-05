package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.model.ControlLink;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkType;
import java.time.Instant;
import java.util.UUID;

public record ControlLinkResponse(
        UUID id,
        UUID controlId,
        ControlLinkTargetType targetType,
        UUID targetEntityId,
        String targetIdentifier,
        ControlLinkType linkType,
        String targetUrl,
        String targetTitle,
        Instant createdAt,
        Instant updatedAt) {

    public static ControlLinkResponse from(ControlLink link) {
        return new ControlLinkResponse(
                link.getId(),
                link.getControl().getId(),
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
