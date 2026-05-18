package com.keplerops.groundcontrol.domain.grcanalysis.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Current-state projection over append-only observation history per GC-L007.
 * Carries the preflight's "Result Contract" fields and the
 * {@link ObservationProjectionMode} discriminator so callers know whether they
 * are looking at asset exposure or control state.
 *
 * <p>Per the preflight, {@code CONTROL_STATE} mode reports effectiveness from
 * the latest {@link com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment}
 * — never from
 * {@link com.keplerops.groundcontrol.domain.controls.state.ControlStatus#OPERATIONAL}.
 */
public record ObservationProjectionResult(
        String analysisKind,
        String project,
        Instant asOf,
        String derivationMethod,
        Inputs inputs,
        List<AssetExposureItem> assetExposures,
        List<ControlStateItem> controlStates,
        List<String> limitations) {

    public record Inputs(String project, Instant asOf, ObservationProjectionMode mode, UUID assetId, UUID controlId) {}

    public record AssetExposureItem(
            UUID assetId,
            String assetUid,
            String assetType,
            String state,
            List<CurrentObservation> currentObservations) {}

    public record CurrentObservation(
            UUID id, String category, String observationKey, Instant observedAt, Instant expiresAt, String state) {}

    public record ControlStateItem(
            UUID controlId,
            String controlUid,
            String controlStatus,
            String designEffectiveness,
            String operatingEffectiveness,
            LocalDate latestAssessedAt,
            String state) {}
}
