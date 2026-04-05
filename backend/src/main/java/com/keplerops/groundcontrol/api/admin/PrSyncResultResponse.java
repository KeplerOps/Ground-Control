package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.PrSyncResult;
import com.keplerops.groundcontrol.domain.requirements.service.SyncError;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrSyncResultResponse(
        UUID syncId,
        Instant syncedAt,
        int prsFetched,
        int prsCreated,
        int prsUpdated,
        int linksUpdated,
        List<SyncError> errors) {

    public static PrSyncResultResponse from(PrSyncResult result) {
        return new PrSyncResultResponse(
                result.syncId(),
                result.syncedAt(),
                result.prsFetched(),
                result.prsCreated(),
                result.prsUpdated(),
                result.linksUpdated(),
                result.errors());
    }
}
