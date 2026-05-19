package com.keplerops.groundcontrol.domain.testcases.service;

import java.util.UUID;

public record CreateTestCaseFolderCommand(
        UUID projectId, UUID parentFolderId, String title, String description, Integer sortOrder) {}
