package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public interface GraphClient {

    void materializeGraph();

    List<String> getAncestors(String uid, int depth);

    List<String> getDescendants(String uid, int depth);

    List<List<String>> findPaths(String sourceUid, String targetUid);
}
