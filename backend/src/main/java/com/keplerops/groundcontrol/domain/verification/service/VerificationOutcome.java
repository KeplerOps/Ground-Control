package com.keplerops.groundcontrol.domain.verification.service;

import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
import java.time.Instant;
import java.util.Map;

/**
 * Result returned by a {@link VerifierAdapter} after executing verification.
 *
 * <p>Fields map directly to {@link CreateVerificationResultCommand} so consumers can persist the
 * outcome via {@link VerificationResultService#create}.
 */
public record VerificationOutcome(
        String prover,
        VerificationStatus result,
        AssuranceLevel assuranceLevel,
        String property,
        Map<String, Object> evidence,
        Instant verifiedAt,
        Instant expiresAt) {}
