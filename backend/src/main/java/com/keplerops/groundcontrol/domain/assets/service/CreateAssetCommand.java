package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import java.util.UUID;

public record CreateAssetCommand(UUID projectId, String uid, String name, String description, AssetType assetType) {}
