package com.keplerops.groundcontrol.domain.testcases.service;

import java.time.LocalDate;

/**
 * Partial-update payload for {@link TestPlanService#update}. Each nullable
 * field follows the TC-001 codex-cycle-1 contract: a {@code null} value means
 * "leave the existing value alone," and the matching {@code clear*} flag
 * lets the caller explicitly clear a stored value (the JSON shape cannot
 * otherwise distinguish "absent" from "null").
 *
 * <p>{@code name} cannot be cleared because it is required by the entity.
 */
public record UpdateTestPlanCommand(
        String name,
        String description,
        String product,
        String version,
        String build,
        LocalDate startDate,
        LocalDate endDate,
        boolean clearDescription,
        boolean clearProduct,
        boolean clearVersion,
        boolean clearBuild,
        boolean clearStartDate,
        boolean clearEndDate) {}
