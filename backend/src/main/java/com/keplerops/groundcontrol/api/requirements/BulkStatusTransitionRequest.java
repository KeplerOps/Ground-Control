package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.state.Status;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BulkStatusTransitionRequest(@NotEmpty List<UUID> ids, @NotNull Status status, String reason) {}
