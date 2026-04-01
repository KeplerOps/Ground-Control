package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.AssetRelationType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssetRelationRequest(@NotNull UUID targetId, @NotNull AssetRelationType relationType) {}
