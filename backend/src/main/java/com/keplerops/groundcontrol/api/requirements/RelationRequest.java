package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.state.RelationType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RelationRequest(@NotNull UUID targetId, @NotNull RelationType relationType) {}
