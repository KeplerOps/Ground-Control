package com.keplerops.groundcontrol.domain.testcases.service;

import java.util.List;
import java.util.UUID;

public record ReorderTestCaseFoldersCommand(UUID parentFolderId, List<UUID> orderedFolderIds) {}
