package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record ReqifParseResult(List<ReqifRequirement> requirements, List<ReqifRelation> relations) {

    public ReqifParseResult {
        requirements = List.copyOf(requirements);
        relations = List.copyOf(relations);
    }
}
