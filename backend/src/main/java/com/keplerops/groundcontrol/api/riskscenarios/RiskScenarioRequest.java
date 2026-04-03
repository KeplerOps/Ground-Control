package com.keplerops.groundcontrol.api.riskscenarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RiskScenarioRequest(
        @NotBlank @Size(max = 20) String uid,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String threatSource,
        @NotBlank String threatEvent,
        @NotBlank String affectedObject,
        String vulnerability,
        @NotBlank String consequence,
        @NotBlank @Size(max = 100) String timeHorizon,
        String observationRefs,
        String topologyContext) {}
