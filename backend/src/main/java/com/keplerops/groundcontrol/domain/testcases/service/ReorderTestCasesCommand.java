package com.keplerops.groundcontrol.domain.testcases.service;

import java.util.List;
import java.util.UUID;

public record ReorderTestCasesCommand(UUID parentFolderId, List<UUID> orderedTestCaseIds) {}
