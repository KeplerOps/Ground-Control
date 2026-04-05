package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;
import java.util.UUID;

public interface GraphClient {

    void materializeGraph();

    List<String> getAncestors(UUID projectId, String uid, int depth);

    List<String> getDescendants(UUID projectId, String uid, int depth);

    List<PathResult> findPaths(UUID projectId, String sourceUid, String targetUid);
}
