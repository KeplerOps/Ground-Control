package com.keplerops.groundcontrol.api.threatmodels;

import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModel;
import com.keplerops.groundcontrol.domain.threatmodels.state.StrideCategory;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelStatus;
import java.time.Instant;
import java.util.UUID;

public record ThreatModelResponse(
        UUID id,
        String graphNodeId,
        String projectIdentifier,
        String uid,
        String title,
        ThreatModelStatus status,
        String threatSource,
        String threatEvent,
        String effect,
        StrideCategory stride,
        String narrative,
        Instant createdAt,
        Instant updatedAt,
        String createdBy) {

    public static ThreatModelResponse from(ThreatModel tm) {
        return new ThreatModelResponse(
                tm.getId(),
                GraphIds.nodeId(GraphEntityType.THREAT_MODEL, tm.getId()),
                tm.getProject().getIdentifier(),
                tm.getUid(),
                tm.getTitle(),
                tm.getStatus(),
                tm.getThreatSource(),
                tm.getThreatEvent(),
                tm.getEffect(),
                tm.getStride(),
                tm.getNarrative(),
                tm.getCreatedAt(),
                tm.getUpdatedAt(),
                tm.getCreatedBy());
    }
}
