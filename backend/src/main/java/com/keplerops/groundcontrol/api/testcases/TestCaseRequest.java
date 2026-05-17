package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TestCaseRequest(
        @NotBlank @Size(max = 50) String uid,
        @NotBlank @Size(max = 200) String title,
        @NotNull TestCaseType type,
        @NotNull TestCasePriority priority,
        // Authored format axis (TC-004 / ADR-042). Optional on the wire so
        // existing clients keep working — the service defaults to STEP_BASED
        // when null.
        TestCaseFormat format,
        String description,
        String preconditions,
        String postconditions,
        @PositiveOrZero Long estimatedDurationSeconds) {}
