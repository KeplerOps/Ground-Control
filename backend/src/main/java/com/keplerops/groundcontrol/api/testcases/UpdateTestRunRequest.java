package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateTestRunRequest(
        @Size(max = 200) String name,
        @Size(max = 100) String environment,
        @Size(max = 100) String version,
        @Size(max = 100) String build,
        Instant startAt,
        Instant endAt,
        Boolean clearEnvironment,
        Boolean clearVersion,
        Boolean clearBuild,
        Boolean clearStartAt,
        Boolean clearEndAt) {}
