package com.keplerops.groundcontrol.domain.testcases.service;

import com.keplerops.groundcontrol.domain.testcases.state.TestRunCaseResultStatus;

/**
 * Partial-update payload for a per-case execution result. {@code status}
 * is required (no clear semantics — a result row always has a status).
 * {@code notes} follows the {@code null = leave alone},
 * {@code clearNotes = wipe to null} contract.
 */
public record UpdateTestRunCaseResultCommand(TestRunCaseResultStatus status, String notes, boolean clearNotes) {}
