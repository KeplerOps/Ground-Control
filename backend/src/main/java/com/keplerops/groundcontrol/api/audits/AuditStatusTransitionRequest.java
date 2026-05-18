package com.keplerops.groundcontrol.api.audits;

import com.keplerops.groundcontrol.domain.audits.state.AuditStatus;
import jakarta.validation.constraints.NotNull;

public record AuditStatusTransitionRequest(@NotNull AuditStatus status) {}
