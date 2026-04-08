package com.keplerops.groundcontrol.domain.verification.service;

import com.keplerops.groundcontrol.domain.verification.state.AssuranceLevel;
import java.util.Map;

/**
 * Input to a {@link VerifierAdapter}. Describes what to verify and at what assurance level.
 *
 * <p>The {@code options} map carries adapter-specific configuration (tool flags, model configs,
 * reviewer info) so the common contract stays stable across all verifier backends.
 */
public record VerificationRequest(
        String targetPath, String property, AssuranceLevel assuranceLevel, Map<String, Object> options) {}
