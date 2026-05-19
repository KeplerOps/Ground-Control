package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddTestSuiteSourceRequirementRequest(@NotNull UUID requirementId) {}
