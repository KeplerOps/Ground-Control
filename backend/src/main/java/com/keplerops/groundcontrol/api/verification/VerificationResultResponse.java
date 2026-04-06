package com.keplerops.groundcontrol.api.verification;

import com.keplerops.groundcontrol.domain.graph.model.GraphEntityType;
import com.keplerops.groundcontrol.domain.graph.model.GraphIds;
import com.keplerops.groundcontrol.domain.verification.model.VerificationResult;
import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record VerificationResultResponse(
        UUID id,
        String graphNodeId,
        String projectIdentifier,
        UUID targetId,
        UUID requirementId,
        String prover,
        String property,
        VerificationStatus result,
        AssuranceLevel assuranceLevel,
        Map<String, Object> evidence,
        Instant verifiedAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt) {

    public static VerificationResultResponse from(VerificationResult vr) {
        return new VerificationResultResponse(
                vr.getId(),
                GraphIds.nodeId(GraphEntityType.VERIFICATION_RESULT, vr.getId()),
                vr.getProject().getIdentifier(),
                vr.getTarget() != null ? vr.getTarget().getId() : null,
                vr.getRequirement() != null ? vr.getRequirement().getId() : null,
                vr.getProver(),
                vr.getProperty(),
                vr.getResult(),
                vr.getAssuranceLevel(),
                vr.getEvidence(),
                vr.getVerifiedAt(),
                vr.getExpiresAt(),
                vr.getCreatedAt(),
                vr.getUpdatedAt());
    }
}
