package com.keplerops.groundcontrol.api.audits;

import com.keplerops.groundcontrol.domain.audits.model.Audit;
import com.keplerops.groundcontrol.domain.audits.state.AuditStatus;
import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditResponse(
        UUID id,
        String graphNodeId,
        String projectIdentifier,
        String uid,
        String title,
        AuditType auditType,
        AuditStatus status,
        String scopeDescription,
        List<String> objectives,
        List<AuditPhaseDto> phases,
        List<String> teamMembers,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {

    public static AuditResponse from(Audit a) {
        return new AuditResponse(
                a.getId(),
                GraphIds.nodeId(GraphEntityType.AUDIT, a.getId()),
                a.getProject().getIdentifier(),
                a.getUid(),
                a.getTitle(),
                a.getAuditType(),
                a.getStatus(),
                a.getScopeDescription(),
                a.getObjectives(),
                a.getPhases().stream().map(AuditPhaseDto::fromDomain).toList(),
                a.getTeamMembers(),
                a.getCreatedBy(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
