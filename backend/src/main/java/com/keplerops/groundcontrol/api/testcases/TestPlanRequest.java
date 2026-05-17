package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TestPlanRequest(
        @NotBlank @Size(max = 50) String uid,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 8192) String description,
        @Size(max = 200) String product,
        @Size(max = 100) String version,
        @Size(max = 100) String build,
        LocalDate startDate,
        LocalDate endDate) {}
