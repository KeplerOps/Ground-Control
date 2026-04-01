package com.keplerops.groundcontrol.api.adrs;

import com.keplerops.groundcontrol.domain.adrs.state.AdrStatus;
import jakarta.validation.constraints.NotNull;

public record AdrStatusTransitionRequest(@NotNull AdrStatus status) {}
