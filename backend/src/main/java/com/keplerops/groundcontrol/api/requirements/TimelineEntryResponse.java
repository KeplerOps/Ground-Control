package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.service.FieldChange;
import com.keplerops.groundcontrol.domain.requirements.service.TimelineEntry;
import com.keplerops.groundcontrol.domain.requirements.state.ChangeCategory;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TimelineEntryResponse(
        int revisionNumber,
        String revisionType,
        Instant timestamp,
        String actor,
        String reason,
        ChangeCategory changeCategory,
        UUID entityId,
        Map<String, Object> snapshot,
        Map<String, FieldChange> changes) {

    public static TimelineEntryResponse from(TimelineEntry entry) {
        return new TimelineEntryResponse(
                entry.revisionNumber(),
                entry.revisionType(),
                entry.timestamp(),
                entry.actor(),
                entry.reason(),
                entry.changeCategory(),
                entry.entityId(),
                entry.snapshot(),
                entry.changes());
    }
}
