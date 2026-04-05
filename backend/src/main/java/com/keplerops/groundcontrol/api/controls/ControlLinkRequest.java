package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ControlLinkRequest(
        @NotNull ControlLinkTargetType targetType,
        UUID targetEntityId,
        @Size(max = 500) String targetIdentifier,
        @NotNull ControlLinkType linkType,
        @Size(max = 2000) String targetUrl,
        @Size(max = 255) String targetTitle) {}
