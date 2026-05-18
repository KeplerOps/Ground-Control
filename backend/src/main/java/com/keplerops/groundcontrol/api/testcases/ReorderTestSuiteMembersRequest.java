package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderTestSuiteMembersRequest(@NotNull @NotEmpty List<UUID> orderedTestCaseIds) {}
