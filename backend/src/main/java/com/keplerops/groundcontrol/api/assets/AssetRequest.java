package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssetRequest(
        @NotBlank @Size(max = 50) String uid,
        @NotBlank @Size(max = 200) String name,
        String description,
        AssetType assetType) {}
