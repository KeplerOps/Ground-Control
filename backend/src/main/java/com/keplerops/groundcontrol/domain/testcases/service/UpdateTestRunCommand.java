package com.keplerops.groundcontrol.domain.testcases.service;

import java.time.Instant;

/**
 * Partial-update payload for {@link TestRunService#update}. Same null /
 * {@code clear*} convention as {@link UpdateTestPlanCommand}: a {@code
 * null} value means "leave the existing value alone," and the matching
 * {@code clear*} flag explicitly clears the stored value. {@code name}
 * cannot be cleared because the entity requires it.
 */
public record UpdateTestRunCommand(
        String name,
        String environment,
        String version,
        String build,
        Instant startAt,
        Instant endAt,
        boolean clearEnvironment,
        boolean clearVersion,
        boolean clearBuild,
        boolean clearStartAt,
        boolean clearEndAt) {}
