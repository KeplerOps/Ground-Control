package com.keplerops.groundcontrol.api.evidence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.keplerops.groundcontrol.domain.evidence.model.EvidenceSourceRef;
import com.keplerops.groundcontrol.domain.evidence.state.EvidenceSourceKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvidenceSourceRefDto(
        @NotNull EvidenceSourceKind sourceKind,
        UUID sourceEntityId,
        @Size(max = 500) String sourceIdentifier,
        @Size(max = 100) String role) {

    public EvidenceSourceRef toDomain() {
        return new EvidenceSourceRef(sourceKind, sourceEntityId, sourceIdentifier, role);
    }

    public static EvidenceSourceRefDto fromDomain(EvidenceSourceRef ref) {
        return new EvidenceSourceRefDto(ref.sourceKind(), ref.sourceEntityId(), ref.sourceIdentifier(), ref.role());
    }
}
