package com.keplerops.groundcontrol.domain.findings.service;

import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import java.time.LocalDate;
import java.util.UUID;

public record CreateFindingCommand(
        UUID projectId,
        String uid,
        String title,
        FindingType findingType,
        FindingSeverity severity,
        String description,
        String rootCauseAnalysis,
        String owner,
        LocalDate dueDate) {}
