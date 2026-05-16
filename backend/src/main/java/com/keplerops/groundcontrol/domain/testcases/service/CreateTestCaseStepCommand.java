package com.keplerops.groundcontrol.domain.testcases.service;

import java.util.UUID;

public record CreateTestCaseStepCommand(
        UUID projectId, UUID testCaseId, int stepNumber, String action, String expectedResult, String actualResult) {}
