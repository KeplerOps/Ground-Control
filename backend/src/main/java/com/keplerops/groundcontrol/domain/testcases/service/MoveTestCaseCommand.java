package com.keplerops.groundcontrol.domain.testcases.service;

import java.util.UUID;

public record MoveTestCaseCommand(UUID parentFolderId, Integer sortOrder) {}
