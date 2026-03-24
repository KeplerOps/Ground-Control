package com.keplerops.groundcontrol.api.triggers;

import com.keplerops.groundcontrol.domain.workflows.state.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TriggerRequest(
        @NotNull UUID workflowId,
        @NotBlank String name,
        @NotNull TriggerType triggerType,
        String config) {}
