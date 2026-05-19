package com.keplerops.groundcontrol.domain.testcases.service;

import java.util.UUID;

public record CopyTestCaseCommand(String newUid, UUID parentFolderId, Integer sortOrder) {}
