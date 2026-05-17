package com.keplerops.groundcontrol.domain.testcases.service;

import java.util.UUID;

public record CreateTestCaseGherkinCommand(UUID projectId, UUID testCaseId, String source) {}
