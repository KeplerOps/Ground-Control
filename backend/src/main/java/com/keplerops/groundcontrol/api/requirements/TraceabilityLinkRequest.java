package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TraceabilityLinkRequest(
        @NotNull ArtifactType artifactType,
        @NotBlank @Size(max = 500) String artifactIdentifier,
        @Size(max = 2000) String artifactUrl,
        @Size(max = 255) String artifactTitle,
        @NotNull LinkType linkType) {}
