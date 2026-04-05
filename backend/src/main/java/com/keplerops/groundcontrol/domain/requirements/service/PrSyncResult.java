package com.keplerops.groundcontrol.domain.requirements.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrSyncResult(
        UUID syncId,
        Instant syncedAt,
        int prsFetched,
        int prsCreated,
        int prsUpdated,
        int linksUpdated,
        List<SyncError> errors) {

    public PrSyncResult {
        errors = List.copyOf(errors);
    }
}
