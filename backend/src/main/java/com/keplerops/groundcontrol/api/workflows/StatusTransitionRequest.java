package com.keplerops.groundcontrol.api.workflows;

import com.keplerops.groundcontrol.domain.workflows.state.WorkflowStatus;
import jakarta.validation.constraints.NotNull;

public record StatusTransitionRequest(@NotNull WorkflowStatus status) {}
