package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

record ParsedRequirement(String uid, String title, String statement, Integer wave, List<String> parentUids) {

    ParsedRequirement {
        parentUids = List.copyOf(parentUids);
    }
}
