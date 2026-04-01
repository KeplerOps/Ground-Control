package com.keplerops.groundcontrol.domain.assets.service;

import com.keplerops.groundcontrol.domain.assets.model.AssetRelation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import java.util.List;

public record AssetSubgraphResult(List<OperationalAsset> assets, List<AssetRelation> relations) {}
