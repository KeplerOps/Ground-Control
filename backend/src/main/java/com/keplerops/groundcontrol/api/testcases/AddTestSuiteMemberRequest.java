package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record AddTestSuiteMemberRequest(@NotNull UUID testCaseId, @PositiveOrZero Integer position) {}
