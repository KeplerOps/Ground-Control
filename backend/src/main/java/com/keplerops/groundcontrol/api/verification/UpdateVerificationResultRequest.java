package com.keplerops.groundcontrol.api.verification;

import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UpdateVerificationResultRequest(
        UUID targetId,
        UUID requirementId,
        @Size(max = 100) String prover,
        String property,
        VerificationStatus result,
        AssuranceLevel assuranceLevel,
        Map<String, Object> evidence,
        Instant verifiedAt,
        Instant expiresAt) {}
