package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Partial-update DTO for a test case. The nullable text/duration fields use the
 * existing project convention from {@code UpdateFindingRequest}: `null` means
 * "no change", an explicit `clearXxx: true` flag means "set to null". Without
 * the clear flags a client that legitimately wants to wipe a description /
 * preconditions / postconditions / estimated duration has no API surface to do
 * so, because the service treats `null` as "leave alone".
 */
public record UpdateTestCaseRequest(
        @Size(max = 200) String title,
        TestCaseType type,
        TestCasePriority priority,
        String description,
        String preconditions,
        String postconditions,
        @PositiveOrZero Long estimatedDurationSeconds,
        Boolean clearDescription,
        Boolean clearPreconditions,
        Boolean clearPostconditions,
        Boolean clearEstimatedDuration) {}
