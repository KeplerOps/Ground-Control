package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.graph.model.GraphProjection;
import java.util.UUID;

public interface MixedGraphClient {

    GraphProjection getVisualization(UUID projectId);
}
