package com.keplerops.groundcontrol.api.verification;

import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record VerificationResultRequest(
        UUID targetId,
        UUID requirementId,
        @NotBlank @Size(max = 100) String prover,
        String property,
        @NotNull VerificationStatus result,
        @NotNull AssuranceLevel assuranceLevel,
        Map<String, Object> evidence,
        @NotNull Instant verifiedAt,
        Instant expiresAt) {}
