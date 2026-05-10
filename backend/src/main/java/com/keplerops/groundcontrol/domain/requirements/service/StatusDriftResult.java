package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.requirements.state.ConfidenceLevel;
import com.keplerops.groundcontrol.domain.requirements.state.StatusDriftSignal;
import java.util.List;

/**
 * Result of a status-drift analysis: {@code DRAFT} requirements that carry
 * independent evidence of implementation or design completion. Read-only derived
 * evidence per ADR-011 §9 — never a traceability edge or a lifecycle change.
 *
 * @param draftRequirementsScanned number of non-archived {@code DRAFT} requirements examined
 * @param minimumConfidence the threshold applied; only findings at or above it are included
 * @param findings flagged requirements, strongest confidence first then UID ascending
 */
public record StatusDriftResult(
        int draftRequirementsScanned, ConfidenceLevel minimumConfidence, List<Finding> findings) {

    /**
     * One flagged {@code DRAFT} requirement.
     *
     * @param confidence the strongest confidence band across {@link #evidence}
     * @param strongestSignal the signal that produced {@link #confidence} (earliest in {@link StatusDriftSignal} order on a tie)
     */
    public record Finding(
            String uid,
            String title,
            ConfidenceLevel confidence,
            StatusDriftSignal strongestSignal,
            List<Evidence> evidence) {}

    /**
     * One piece of evidence backing a {@link Finding}.
     *
     * @param artifactType the artifact kind ({@code ArtifactType} name, e.g. {@code GITHUB_ISSUE}, {@code ADR}, {@code CODE_FILE})
     * @param artifactIdentifier canonical identifier per ADR-011 conventions (decimal for issues/PRs, ADR UID for ADRs, repo-relative path for code)
     * @param artifactTitle human title where known, else empty
     * @param artifactUrl URL where known, else empty
     * @param detail short human note about the match (e.g. {@code "issue CLOSED"}, {@code "ADR ACCEPTED"})
     */
    public record Evidence(
            StatusDriftSignal signal,
            ConfidenceLevel confidence,
            String artifactType,
            String artifactIdentifier,
            String artifactTitle,
            String artifactUrl,
            String detail) {}
}
