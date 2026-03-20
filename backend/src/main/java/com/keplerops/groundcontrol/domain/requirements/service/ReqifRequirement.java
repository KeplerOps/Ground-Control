package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record ReqifRequirement(String identifier, String title, String statement, List<String> parentIdentifiers) {

    public ReqifRequirement {
        parentIdentifiers = List.copyOf(parentIdentifiers);
    }
}
