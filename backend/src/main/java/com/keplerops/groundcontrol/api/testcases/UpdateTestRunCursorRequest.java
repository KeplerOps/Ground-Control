package com.keplerops.groundcontrol.api.testcases;

import java.util.UUID;

public record UpdateTestRunCursorRequest(UUID currentCaseResultId, UUID currentStepResultId, Boolean clearCursor) {}
