package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
import jakarta.validation.constraints.Size;

/**
 * TC-008 / ADR-049 — partial-update request for {@code TestRunCaseResult}.
 *
 * <p>{@code status} is intentionally <em>not</em> {@code @NotNull}. The TC-009
 * runner UI autosaves case notes whenever the textarea blurs, and a request
 * with {@code status} pinned to whatever the local component rendered would
 * silently overwrite a concurrent status flip the React Query cache had not
 * yet refetched (issue #927 codex review, cycle 1). When {@code status} is
 * absent the service preserves the existing value; only an explicitly
 * supplied status mutates the field.
 */
public record UpdateTestRunCaseResultRequest(
        TestRunCaseResultStatus status, @Size(max = 8192) String notes, Boolean clearNotes) {}
