package com.keplerops.groundcontrol.api.testcases;

import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * TC-009 / ADR-050 — partial-update request for {@code TestRunStepResult}.
 *
 * <p>{@code status} is intentionally <em>not</em> {@code @NotNull}. The
 * runner UI autosaves step comments whenever the textarea blurs, and a
 * request with {@code status} pinned to whatever the local component
 * rendered would silently overwrite a concurrent status flip the React
 * Query cache had not yet refetched (codex review cycle 1). When
 * {@code status} is absent the service preserves the existing value; only
 * an explicitly supplied status mutates the field. {@code executedAt} is
 * server-defaulted to "now" when status becomes non-NOT_RUN and no explicit
 * timestamp is supplied, so every observed step carries a timestamp
 * regardless of which client (SPA, MCP, raw REST) drove the update.
 */
public record UpdateTestRunStepResultRequest(
        TestRunCaseResultStatus status,
        @Size(max = 8192) String comment,
        Boolean clearComment,
        Instant executedAt,
        Boolean clearExecutedAt) {}
