package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;
import java.time.Instant;

/**
 * TC-009 / ADR-050 — Update a single {@code TestRunStepResult}'s runtime
 * fields. {@code clearComment} / {@code clearExecutedAt} let the caller
 * null a previously-set value explicitly without conflating "leave alone"
 * (incoming null) with "clear" — same nullable-field protocol the rest of
 * the test-run service surface uses.
 */
public record UpdateTestRunStepResultCommand(
        TestRunCaseResultStatus status,
        String comment,
        boolean clearComment,
        Instant executedAt,
        boolean clearExecutedAt) {}
