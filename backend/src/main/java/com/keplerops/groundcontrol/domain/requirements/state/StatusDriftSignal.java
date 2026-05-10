package com.keplerops.groundcontrol.domain.requirements.state;

/**
 * Independent, project-scoped evidence signals that a {@code DRAFT} requirement may
 * already be implemented or design-finalized. Ordered strongest-first so that, on a
 * confidence tie, the earliest constant is reported as the "strongest signal".
 *
 * <p>Per ADR-011 §9 these are read-only derived signals; they never create
 * traceability links or transition requirements. Every signal is derived from data
 * owned by the requirement's own project — its canonical traceability links and
 * accepted ADR records — so a project-scoped analysis never reads project- or
 * repo-unscoped global caches.
 */
@SuppressWarnings("java:S115") // names mirror the documented signal taxonomy, not Java constant style
public enum StatusDriftSignal {

    /** An {@code IMPLEMENTS} traceability link exists on a {@code DRAFT} requirement (anomalous; the GC-O007 / #794 shape). */
    IMPLEMENTS_LINK_ON_DRAFT,

    /** A {@code DOCUMENTS} link points at an ADR (same project) in {@code ACCEPTED} status. */
    ACCEPTED_ADR_DOCUMENTS_LINK,

    /** A non-{@code IMPLEMENTS} traceability link points at a GitHub issue. */
    LINKED_GITHUB_ISSUE,

    /** A non-{@code IMPLEMENTS} traceability link points at a GitHub pull request. */
    LINKED_PULL_REQUEST,

    /** A non-{@code IMPLEMENTS} traceability link points at a code, test, spec, or proof artifact. */
    LINKED_CODE_ARTIFACT,

    /** A non-{@code IMPLEMENTS} traceability link points at a documentation, config, or policy artifact. */
    LINKED_DOC_ARTIFACT;

    /** The default confidence band this signal contributes when matched. */
    public ConfidenceLevel defaultConfidence() {
        return switch (this) {
            case IMPLEMENTS_LINK_ON_DRAFT -> ConfidenceLevel.HIGH;
            case ACCEPTED_ADR_DOCUMENTS_LINK, LINKED_GITHUB_ISSUE, LINKED_PULL_REQUEST -> ConfidenceLevel.MEDIUM;
            case LINKED_CODE_ARTIFACT, LINKED_DOC_ARTIFACT -> ConfidenceLevel.LOW;
        };
    }
}
