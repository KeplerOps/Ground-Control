package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenarioLink;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkType;
import java.time.Instant;
import java.util.UUID;

public record RiskScenarioLinkResponse(
        UUID id,
        UUID riskScenarioId,
        RiskScenarioLinkTargetType targetType,
        UUID targetEntityId,
        String targetIdentifier,
        RiskScenarioLinkType linkType,
        String targetUrl,
        String targetTitle,
        Instant createdAt,
        Instant updatedAt) {

    public static RiskScenarioLinkResponse from(RiskScenarioLink link) {
        return new RiskScenarioLinkResponse(
                link.getId(),
                link.getRiskScenario().getId(),
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
