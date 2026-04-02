package com.keplerops.groundcontrol.domain.riskscenarios.service;

import java.util.UUID;

public record CreateRiskScenarioCommand(
        UUID projectId,
        String uid,
        String title,
        String threatSource,
        String threatEvent,
        String affectedObject,
        String vulnerability,
        String consequence,
        String timeHorizon,
        String observationRefs,
        String topologyContext) {}
