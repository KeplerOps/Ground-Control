package com.keplerops.groundcontrol.api.evidence;

import com.keplerops.groundcontrol.domain.evidence.state.EvidenceType;
import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record EvidenceArtifactRequest(
        @NotBlank @Size(max = 50) String uid,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 8000) String summary,
        @NotNull EvidenceType evidenceType,
        @NotBlank @Size(max = 200) String derivationMethod,
        @NotNull Instant derivedAt,
        AssuranceLevel assuranceLevel,
        @Size(max = 50) String confidence,
        @Size(max = 4000) String notes,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid EvidenceSourceRefDto> sources) {}
