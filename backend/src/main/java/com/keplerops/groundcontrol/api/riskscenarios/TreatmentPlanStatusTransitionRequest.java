package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.riskscenarios.state.TreatmentPlanStatus;
import jakarta.validation.constraints.NotNull;

public record TreatmentPlanStatusTransitionRequest(@NotNull TreatmentPlanStatus status) {}
