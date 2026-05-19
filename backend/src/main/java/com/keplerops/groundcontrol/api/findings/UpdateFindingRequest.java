package com.keplerops.groundcontrol.api.findings;

import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateFindingRequest(
        @Size(max = 200) String title,
        FindingType findingType,
        FindingSeverity severity,
        String description,
        String rootCauseAnalysis,
        @Size(max = 100) String owner,
        LocalDate dueDate,
        Boolean clearRootCauseAnalysis,
        Boolean clearOwner,
        Boolean clearDueDate) {}
