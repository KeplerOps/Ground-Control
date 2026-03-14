package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.SyncResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SyncResultResponse(
        UUID syncId,
        Instant syncedAt,
        int issuesFetched,
        int issuesCreated,
        int issuesUpdated,
        int linksUpdated,
        List<Map<String, Object>> errors) {

    public static SyncResultResponse from(SyncResult result) {
        return new SyncResultResponse(
                result.syncId(),
                result.syncedAt(),
                result.issuesFetched(),
                result.issuesCreated(),
                result.issuesUpdated(),
                result.linksUpdated(),
                result.errors());
    }
}
