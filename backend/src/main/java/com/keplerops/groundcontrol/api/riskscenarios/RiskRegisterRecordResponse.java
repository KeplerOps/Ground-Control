package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskRegisterRecord;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskRegisterStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RiskRegisterRecordResponse(
        UUID id,
        String graphNodeId,
        String projectIdentifier,
        String uid,
        String title,
        String owner,
        RiskRegisterStatus status,
        String reviewCadence,
        Instant nextReviewAt,
        List<String> categoryTags,
        Map<String, Object> decisionMetadata,
        String assetScopeSummary,
        List<UUID> riskScenarioIds,
        List<String> riskScenarioUids,
        Instant createdAt,
        Instant updatedAt) {

    public static RiskRegisterRecordResponse from(RiskRegisterRecord record) {
        return new RiskRegisterRecordResponse(
                record.getId(),
                GraphIds.nodeId(GraphEntityType.RISK_REGISTER_RECORD, record.getId()),
                record.getProject().getIdentifier(),
                record.getUid(),
                record.getTitle(),
                record.getOwner(),
                record.getStatus(),
                record.getReviewCadence(),
                record.getNextReviewAt(),
                record.getCategoryTags(),
                record.getDecisionMetadata(),
                record.getAssetScopeSummary(),
                record.getRiskScenarios().stream().map(s -> s.getId()).toList(),
                record.getRiskScenarios().stream().map(s -> s.getUid()).toList(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
