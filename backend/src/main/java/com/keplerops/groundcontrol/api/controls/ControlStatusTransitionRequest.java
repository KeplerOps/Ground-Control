package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.state.ControlStatus;
import jakarta.validation.constraints.NotNull;

public record ControlStatusTransitionRequest(@NotNull ControlStatus status) {}
