package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import jakarta.validation.constraints.Size;

public record UpdateAssetRequest(@Size(max = 200) String name, String description, AssetType assetType) {}
