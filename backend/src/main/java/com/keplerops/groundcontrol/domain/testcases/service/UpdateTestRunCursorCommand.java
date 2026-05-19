package com.keplerops.groundcontrol.domain.testcases.service;

import java.util.UUID;

/**
 * TC-009 / ADR-050 — Update the pause/resume cursor on a {@code TestRun}.
 * When {@code clearCursor} is true the cursor is nulled regardless of the
 * incoming UUIDs (the runner UI calls this to mark the end of a run); when
 * false, the service validates that both UUIDs (when supplied) resolve to
 * the run's own case-result / step-result lineage before persisting.
 */
public record UpdateTestRunCursorCommand(UUID currentCaseResultId, UUID currentStepResultId, boolean clearCursor) {}
