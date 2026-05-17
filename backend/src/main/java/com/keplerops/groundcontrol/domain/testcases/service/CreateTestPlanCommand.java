package com.keplerops.groundcontrol.domain.testcases.service;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTestPlanCommand(
        UUID projectId,
        String uid,
        String name,
        String description,
        String product,
        String version,
        String build,
        LocalDate startDate,
        LocalDate endDate) {}
