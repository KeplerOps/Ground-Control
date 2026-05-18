package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTestRunCaseResultRequest(
        @NotNull TestRunCaseResultStatus status, @Size(max = 8192) String notes, Boolean clearNotes) {}
