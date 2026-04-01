package com.keplerops.groundcontrol.api.adrs;

import com.keplerops.groundcontrol.domain.adrs.model.ArchitectureDecisionRecord;
import com.keplerops.groundcontrol.domain.adrs.state.AdrStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdrResponse(
        UUID id,
        String projectIdentifier,
        String uid,
        String title,
        AdrStatus status,
        LocalDate decisionDate,
        String context,
        String decision,
        String consequences,
        String supersededBy,
        Instant createdAt,
        Instant updatedAt,
        String createdBy) {

    public static AdrResponse from(ArchitectureDecisionRecord adr) {
        return new AdrResponse(
                adr.getId(),
                adr.getProject().getIdentifier(),
                adr.getUid(),
                adr.getTitle(),
                adr.getStatus(),
                adr.getDecisionDate(),
                adr.getContext(),
                adr.getDecision(),
                adr.getConsequences(),
                adr.getSupersededBy(),
                adr.getCreatedAt(),
                adr.getUpdatedAt(),
                adr.getCreatedBy());
    }
}
