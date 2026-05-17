package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Partial-update DTO for a test plan. Mirrors the
 * {@link UpdateTestCaseRequest} convention: a {@code null} field means
 * "no change" and a matching {@code clearXxx: true} flag means "set to
 * null." {@code name} cannot be cleared (the entity requires it).
 */
public record UpdateTestPlanRequest(
        @Size(max = 200) String name,
        @Size(max = 8192) String description,
        @Size(max = 200) String product,
        @Size(max = 100) String version,
        @Size(max = 100) String build,
        LocalDate startDate,
        LocalDate endDate,
        Boolean clearDescription,
        Boolean clearProduct,
        Boolean clearVersion,
        Boolean clearBuild,
        Boolean clearStartDate,
        Boolean clearEndDate) {}
