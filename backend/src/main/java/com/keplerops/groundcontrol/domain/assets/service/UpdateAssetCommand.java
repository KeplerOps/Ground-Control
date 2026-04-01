package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetType;

public record UpdateAssetCommand(String name, String description, AssetType assetType) {}
