package com.keplerops.groundcontrol.domain.requirements.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SyncResult(
        UUID syncId,
        Instant syncedAt,
        int issuesFetched,
        int issuesCreated,
        int issuesUpdated,
        int linksUpdated,
        List<SyncError> errors) {

    public SyncResult {
        errors = List.copyOf(errors);
    }
}
