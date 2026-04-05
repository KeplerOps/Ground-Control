package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RiskScenarioLinkRequest(
        @NotNull RiskScenarioLinkTargetType targetType,
        UUID targetEntityId,
        @Size(max = 500) String targetIdentifier,
        @NotNull RiskScenarioLinkType linkType,
        @Size(max = 2000) String targetUrl,
        @Size(max = 255) String targetTitle) {}
