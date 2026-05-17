package com.keplerops.groundcontrol.domain.evidence.state;

/**
 * Semantic role of an {@code EvidenceArtifact} per GC-M016 / ADR-045.
 *
 * <p>OBSERVATION_SUMMARY rolls up observations. CONTROL_TEST_SUMMARY rolls up
 * control tests. ASSURANCE_CONCLUSION captures a derived assurance judgment
 * (typically over effectiveness assessments and verification results).
 * VERIFICATION_SUMMARY rolls up requirement verification evidence. ATTESTATION
 * is a signed declaration treated as durable evidence. MIXED is the
 * cross-source variant used when a single summary spans multiple kinds.
 */
public enum EvidenceType {
    OBSERVATION_SUMMARY,
    CONTROL_TEST_SUMMARY,
    ASSURANCE_CONCLUSION,
    VERIFICATION_SUMMARY,
    ATTESTATION,
    MIXED
}
