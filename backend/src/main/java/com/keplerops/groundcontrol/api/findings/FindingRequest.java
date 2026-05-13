package com.keplerops.groundcontrol.api.findings;

import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FindingRequest(
        @NotBlank @Size(max = 30) String uid,
        @NotBlank @Size(max = 200) String title,
        @NotNull FindingType findingType,
        @NotNull FindingSeverity severity,
        @NotBlank String description,
        String rootCauseAnalysis,
        @Size(max = 100) String owner,
        LocalDate dueDate) {}
