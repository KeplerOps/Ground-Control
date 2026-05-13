package com.keplerops.groundcontrol.api.findings;

import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import jakarta.validation.constraints.NotNull;

public record FindingStatusTransitionRequest(@NotNull FindingStatus status) {}
