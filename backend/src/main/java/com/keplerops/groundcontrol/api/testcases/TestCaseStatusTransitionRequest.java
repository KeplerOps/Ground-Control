package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.state.TestCaseStatus;
import jakarta.validation.constraints.NotNull;

public record TestCaseStatusTransitionRequest(@NotNull TestCaseStatus status) {}
