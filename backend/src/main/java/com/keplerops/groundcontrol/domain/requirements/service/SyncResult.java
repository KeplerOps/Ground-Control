package com.keplerops.groundcontrol.domain.requirements.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SyncResult(
        UUID syncId,
        Instant syncedAt,
        int issuesFetched,
        int issuesCreated,
        int issuesUpdated,
        int linksUpdated,
        List<Map<String, Object>> errors) {

    public SyncResult {
        errors = List.copyOf(errors);
    }
}
