package com.keplerops.groundcontrol.api.threatmodels;

import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelStatus;
import jakarta.validation.constraints.NotNull;

public record ThreatModelStatusTransitionRequest(@NotNull ThreatModelStatus status) {}
