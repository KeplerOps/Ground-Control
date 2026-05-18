package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.state.TestRunStatus;
import jakarta.validation.constraints.NotNull;

public record TestRunStatusTransitionRequest(@NotNull TestRunStatus status) {}
