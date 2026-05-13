package com.keplerops.groundcontrol.api.findings;

import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FindingLinkRequest(
        @NotNull FindingLinkTargetType targetType,
        UUID targetEntityId,
        @Size(max = 500) String targetIdentifier,
        @NotNull FindingLinkType linkType,
        @Size(max = 2000) String targetUrl,
        @Size(max = 255) String targetTitle) {}
