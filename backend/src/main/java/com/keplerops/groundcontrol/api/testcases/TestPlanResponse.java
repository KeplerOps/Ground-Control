package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.state.TestPlanStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TestPlanResponse(
        UUID id,
        String projectIdentifier,
        String uid,
        String name,
        String description,
        String product,
        String version,
        String build,
        TestPlanStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt) {

    public static TestPlanResponse from(TestPlan plan) {
        return new TestPlanResponse(
                plan.getId(),
                plan.getProject().getIdentifier(),
                plan.getUid(),
                plan.getName(),
                plan.getDescription(),
                plan.getProduct(),
                plan.getVersion(),
                plan.getBuild(),
                plan.getStatus(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getCreatedAt(),
                plan.getUpdatedAt());
    }
}
