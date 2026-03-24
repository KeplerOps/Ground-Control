package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.RecentChange;
import java.time.Instant;

public record RecentChangeResponse(
        String uid, String title, String revisionType, Instant timestamp, String actor, String reason) {

    public static RecentChangeResponse from(RecentChange c) {
        return new RecentChangeResponse(c.uid(), c.title(), c.revisionType(), c.timestamp(), c.actor(), c.reason());
    }
}
