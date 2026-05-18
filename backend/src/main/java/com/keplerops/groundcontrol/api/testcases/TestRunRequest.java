package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record TestRunRequest(
        @NotBlank @Size(max = 50) String uid,
        @NotBlank @Size(max = 200) String name,
        @NotNull UUID testPlanId,
        @NotNull UUID testSuiteId,
        @Size(max = 100) String environment,
        @Size(max = 100) String version,
        @Size(max = 100) String build,
        Instant startAt,
        Instant endAt) {}
