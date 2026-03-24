package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.state.Status;
import jakarta.validation.constraints.NotNull;

public record StatusTransitionRequest(@NotNull Status status, String reason) {}
