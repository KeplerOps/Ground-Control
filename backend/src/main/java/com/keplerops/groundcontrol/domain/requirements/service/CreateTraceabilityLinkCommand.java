package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;

public record CreateTraceabilityLinkCommand(
        ArtifactType artifactType,
        String artifactIdentifier,
        String artifactUrl,
        String artifactTitle,
        LinkType linkType) {}
