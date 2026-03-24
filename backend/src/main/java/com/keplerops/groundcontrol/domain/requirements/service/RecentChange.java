package com.keplerops.groundcontrol.domain.requirements.service;

import java.time.Instant;

public record RecentChange(
        String uid, String title, String revisionType, Instant timestamp, String actor, String reason) {}
