package com.keplerops.groundcontrol.domain.verification.service;

/**
 * Port interface for pluggable verification tool integration (ADR-014 section 6).
 *
 * <p>Each adapter wraps a single external verifier (OpenJML, TLA+/TLC, OPA/Rego, Frama-C, manual
 * review, etc.) behind a common contract. Adapters are execution boundaries only — they must not
 * depend on repositories or persist {@link
 * com.keplerops.groundcontrol.domain.verification.model.VerificationResult} entities directly.
 * Result persistence stays in {@link VerificationResultService}.
 *
 * <p>Infrastructure implementations live in {@code infrastructure/verifiers/} and register as
 * Spring components. The canonical verifier selector is the {@link #proverName()} string, which
 * must match the {@code prover} field persisted on VerificationResult.
 */
public interface VerifierAdapter {

    /**
     * Canonical prover identifier stored in {@code VerificationResult.prover}.
     *
     * <p>Examples: {@code "openjml-esc"}, {@code "tlaplus-tlc"}, {@code "opa"}, {@code "frama-c"},
     * {@code "manual-review"}.
     */
    String proverName();

    /** Whether this adapter is configured and ready to execute verifications. */
    boolean isAvailable();

    /**
     * Execute a verification and return the outcome.
     *
     * <p>Tool-specific output belongs in {@link VerificationOutcome#evidence()}. Implementations
     * must not shell out with string-concatenated commands or log secrets/proof artifacts.
     *
     * @param request what to verify
     * @return the verification outcome
     */
    VerificationOutcome verify(VerificationRequest request);
}
