package com.keplerops.groundcontrol.domain.verification.service;

import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreateVerificationResultCommand(
        UUID projectId,
        UUID targetId,
        UUID requirementId,
        String prover,
        String property,
        VerificationStatus result,
        AssuranceLevel assuranceLevel,
        Map<String, Object> evidence,
        Instant verifiedAt,
        Instant expiresAt) {}
