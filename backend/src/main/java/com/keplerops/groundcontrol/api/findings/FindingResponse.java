package com.keplerops.groundcontrol.api.findings;

import com.keplerops.groundcontrol.domain.findings.model.Finding;
import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FindingResponse(
        UUID id,
        String graphNodeId,
        String projectIdentifier,
        String uid,
        String title,
        FindingType findingType,
        FindingSeverity severity,
        FindingStatus status,
        String description,
        String rootCauseAnalysis,
        String owner,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt,
        String createdBy) {

    public static FindingResponse from(Finding f) {
        return new FindingResponse(
                f.getId(),
                GraphIds.nodeId(GraphEntityType.FINDING, f.getId()),
                f.getProject().getIdentifier(),
                f.getUid(),
                f.getTitle(),
                f.getFindingType(),
                f.getSeverity(),
                f.getStatus(),
                f.getDescription(),
                f.getRootCauseAnalysis(),
                f.getOwner(),
                f.getDueDate(),
                f.getCreatedAt(),
                f.getUpdatedAt(),
                f.getCreatedBy());
    }
}
