package com.keplerops.groundcontrol.api.audits;

import com.keplerops.groundcontrol.domain.audits.state.AuditLinkTargetType;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AuditLinkRequest(
        @NotNull AuditLinkTargetType targetType,
        UUID targetEntityId,
        @Size(max = 500) String targetIdentifier,
        @NotNull AuditLinkType linkType,
        @Size(max = 2000) String targetUrl,
        @Size(max = 255) String targetTitle) {}
