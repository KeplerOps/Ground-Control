package com.keplerops.groundcontrol.domain.documents.service;

import java.util.List;

/** Requirement data needed for StrictDoc export, decoupled from JPA entities. */
public record RequirementExportData(
        String uid, String title, String statement, String comment, List<String> parentUids) {

    public RequirementExportData {
        parentUids = List.copyOf(parentUids);
    }
}
