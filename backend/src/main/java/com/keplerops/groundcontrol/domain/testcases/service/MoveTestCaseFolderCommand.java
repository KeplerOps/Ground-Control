package com.keplerops.groundcontrol.domain.testcases.service;

import java.util.UUID;

public record MoveTestCaseFolderCommand(UUID parentFolderId, Integer sortOrder) {}
