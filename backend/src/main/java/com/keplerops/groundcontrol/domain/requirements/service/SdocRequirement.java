package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record SdocRequirement(
        String uid,
        String title,
        String statement,
        String comment,
        List<Integer> issueRefs,
        List<String> parentUids,
        Integer wave) {

    public SdocRequirement {
        issueRefs = List.copyOf(issueRefs);
        parentUids = List.copyOf(parentUids);
    }
}
