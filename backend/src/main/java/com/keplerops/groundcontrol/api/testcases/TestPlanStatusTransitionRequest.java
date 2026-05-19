package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.state.TestPlanStatus;
import jakarta.validation.constraints.NotNull;

public record TestPlanStatusTransitionRequest(@NotNull TestPlanStatus status) {}
