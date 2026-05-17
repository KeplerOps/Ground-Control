package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderTestCasesRequest(UUID parentFolderId, @NotNull List<UUID> orderedTestCaseIds) {}
