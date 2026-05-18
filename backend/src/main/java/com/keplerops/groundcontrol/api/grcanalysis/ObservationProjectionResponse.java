package com.keplerops.groundcontrol.api.grcanalysis;

import com.keplerops.groundcontrol.domain.grcanalysis.service.ObservationProjectionMode;
import com.keplerops.groundcontrol.domain.grcanalysis.service.ObservationProjectionResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API DTO for observation-projection analysis. Decouples the public JSON
 * contract from the domain service record so future domain refactors do not
 * silently change the wire shape.
 */
public record ObservationProjectionResponse(
        String analysisKind,
        String project,
        Instant asOf,
        String derivationMethod,
        Inputs inputs,
        List<AssetExposureItem> assetExposures,
        List<ControlStateItem> controlStates,
        List<String> limitations) {

    public static ObservationProjectionResponse from(ObservationProjectionResult result) {
        return new ObservationProjectionResponse(
                result.analysisKind(),
                result.project(),
                result.asOf(),
                result.derivationMethod(),
                Inputs.from(result.inputs()),
                result.assetExposures().stream().map(AssetExposureItem::from).toList(),
                result.controlStates().stream().map(ControlStateItem::from).toList(),
                List.copyOf(result.limitations()));
    }

    public record Inputs(String project, Instant asOf, ObservationProjectionMode mode, UUID assetId, UUID controlId) {

        public static Inputs from(ObservationProjectionResult.Inputs inputs) {
            return new Inputs(inputs.project(), inputs.asOf(), inputs.mode(), inputs.assetId(), inputs.controlId());
        }
    }

    public record AssetExposureItem(
            UUID assetId,
            String assetUid,
            String assetType,
            String state,
            List<CurrentObservation> currentObservations) {

        public static AssetExposureItem from(ObservationProjectionResult.AssetExposureItem item) {
            return new AssetExposureItem(
                    item.assetId(),
                    item.assetUid(),
                    item.assetType(),
                    item.state(),
                    item.currentObservations().stream()
                            .map(CurrentObservation::from)
                            .toList());
        }
    }

    public record CurrentObservation(
            UUID id, String category, String observationKey, Instant observedAt, Instant expiresAt, String state) {

        public static CurrentObservation from(ObservationProjectionResult.CurrentObservation obs) {
            return new CurrentObservation(
                    obs.id(), obs.category(), obs.observationKey(), obs.observedAt(), obs.expiresAt(), obs.state());
        }
    }

    public record ControlStateItem(
            UUID controlId,
            String controlUid,
            String controlStatus,
            String designEffectiveness,
            String operatingEffectiveness,
            LocalDate latestAssessedAt,
            String state) {

        public static ControlStateItem from(ObservationProjectionResult.ControlStateItem item) {
            return new ControlStateItem(
                    item.controlId(),
                    item.controlUid(),
                    item.controlStatus(),
                    item.designEffectiveness(),
                    item.operatingEffectiveness(),
                    item.latestAssessedAt(),
                    item.state());
        }
    }
}
