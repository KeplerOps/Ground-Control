package com.keplerops.groundcontrol.domain.riskscenarios.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateRiskAssessmentResultCommand(
        UUID projectId,
        UUID riskScenarioId,
        UUID riskRegisterRecordId,
        UUID methodologyProfileId,
        String analystIdentity,
        String assumptions,
        Map<String, Object> inputFactors,
        Instant observationDate,
        Instant assessmentAt,
        String timeHorizon,
        String confidence,
        Map<String, Object> uncertaintyMetadata,
        Map<String, Object> computedOutputs,
        List<String> evidenceRefs,
        String notes,
        List<UUID> observationIds) {}
