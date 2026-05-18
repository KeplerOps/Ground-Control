package com.keplerops.groundcontrol.domain.testcases.service;

import java.time.Instant;
import java.util.UUID;

public record CreateTestRunCommand(
        UUID projectId,
        String uid,
        String name,
        UUID testPlanId,
        UUID testSuiteId,
        String environment,
        String version,
        String build,
        Instant startAt,
        Instant endAt) {}
