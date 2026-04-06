package com.keplerops.groundcontrol.domain.controls.service;

import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkType;
import java.util.UUID;

public record CreateControlLinkCommand(
        ControlLinkTargetType targetType,
        UUID targetEntityId,
        String targetIdentifier,
        ControlLinkType linkType,
        String targetUrl,
        String targetTitle) {}
