package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
import jakarta.validation.constraints.NotNull;

public record RiskScenarioStatusTransitionRequest(@NotNull RiskScenarioStatus status) {}
