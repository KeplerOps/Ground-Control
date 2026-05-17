package com.keplerops.groundcontrol.domain.evidence.state;

/**
 * Source-reference seam per GC-M016 / ADR-045.
 *
 * <p>Internal kinds resolve to first-class entities by project-scoped UUID
 * ({@code sourceEntityId} on {@link
 * com.keplerops.groundcontrol.domain.evidence.model.EvidenceSourceRef}).
 * External kinds carry a canonical identifier ({@code sourceIdentifier}).
 * Exactly one shape is set per source ref; the service validates this.
 */
public enum EvidenceSourceKind {
    OBSERVATION(true),
    CONTROL_TEST(true),
    CONTROL_EFFECTIVENESS_ASSESSMENT(true),
    VERIFICATION_RESULT(true),
    RISK_ASSESSMENT_RESULT(true),
    FINDING(true),
    ATTESTATION(false),
    EXTERNAL(false);

    private final boolean internal;

    EvidenceSourceKind(boolean internal) {
        this.internal = internal;
    }

    public boolean isInternal() {
        return internal;
    }
}
