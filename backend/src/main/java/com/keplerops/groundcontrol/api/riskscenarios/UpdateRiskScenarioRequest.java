package com.keplerops.groundcontrol.api.riskscenarios;

import jakarta.validation.constraints.Size;

public record UpdateRiskScenarioRequest(
        @Size(max = 200) String title,
        String threatSource,
        String threatEvent,
        String affectedObject,
        String vulnerability,
        String consequence,
        @Size(max = 100) String timeHorizon,
        String observationRefs,
        String topologyContext) {}
