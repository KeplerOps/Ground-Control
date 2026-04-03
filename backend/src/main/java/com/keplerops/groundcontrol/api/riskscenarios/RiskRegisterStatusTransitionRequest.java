package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskRegisterStatus;
import jakarta.validation.constraints.NotNull;

public record RiskRegisterStatusTransitionRequest(@NotNull RiskRegisterStatus status) {}
