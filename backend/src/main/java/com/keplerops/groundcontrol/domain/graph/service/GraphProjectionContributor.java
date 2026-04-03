package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.graph.model.GraphEdge;
import com.keplerops.groundcontrol.domain.graph.model.GraphNode;
import java.util.List;
import java.util.UUID;

public interface GraphProjectionContributor {

    List<GraphNode> contributeNodes(UUID projectId);

    List<GraphEdge> contributeEdges(UUID projectId);
}
