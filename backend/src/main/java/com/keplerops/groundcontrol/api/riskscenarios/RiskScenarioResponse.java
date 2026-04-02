package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
import java.time.Instant;
import java.util.UUID;

public record RiskScenarioResponse(
        UUID id,
        String projectIdentifier,
        String uid,
        String title,
        RiskScenarioStatus status,
        String threatSource,
        String threatEvent,
        String affectedObject,
        String vulnerability,
        String consequence,
        String timeHorizon,
        String observationRefs,
        String topologyContext,
        Instant createdAt,
        Instant updatedAt,
        String createdBy) {

    public static RiskScenarioResponse from(RiskScenario rs) {
        return new RiskScenarioResponse(
                rs.getId(),
                rs.getProject().getIdentifier(),
                rs.getUid(),
                rs.getTitle(),
                rs.getStatus(),
                rs.getThreatSource(),
                rs.getThreatEvent(),
                rs.getAffectedObject(),
                rs.getVulnerability(),
                rs.getConsequence(),
                rs.getTimeHorizon(),
                rs.getObservationRefs(),
                rs.getTopologyContext(),
                rs.getCreatedAt(),
                rs.getUpdatedAt(),
                rs.getCreatedBy());
    }
}
