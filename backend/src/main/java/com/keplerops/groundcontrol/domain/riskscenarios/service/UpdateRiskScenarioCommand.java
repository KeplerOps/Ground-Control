package com.keplerops.groundcontrol.domain.riskscenarios.service;

public record UpdateRiskScenarioCommand(
        String title,
        String threatSource,
        String threatEvent,
        String affectedObject,
        String vulnerability,
        String consequence,
        String timeHorizon,
        String observationRefs,
        String topologyContext) {}
