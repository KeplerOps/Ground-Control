package com.keplerops.groundcontrol.domain.evidence.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.keplerops.groundcontrol.domain.evidence.state.EvidenceSourceKind;
import java.util.UUID;

/**
 * Dual-mode source reference on an {@link EvidenceArtifact} per ADR-045.
 *
 * <p>For internal kinds (see {@link EvidenceSourceKind#isInternal()})
 * {@code sourceEntityId} carries the UUID of a project-scoped first-class
 * entity (Observation, ControlTest, ControlEffectivenessAssessment,
 * VerificationResult, RiskAssessmentResult, Finding). For external kinds
 * (ATTESTATION, EXTERNAL) {@code sourceIdentifier} carries an opaque
 * canonical identifier the system does not own (e.g. a third-party
 * attestation URL, a scanner finding key). The service enforces that exactly
 * one of the two is set per kind.
 *
 * <p>{@code role} is a free-text annotation (e.g. {@code "primary"},
 * {@code "supporting"}) preserved on the artifact for provenance; it is not
 * validated semantically.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvidenceSourceRef(
        EvidenceSourceKind sourceKind, UUID sourceEntityId, String sourceIdentifier, String role) {}
