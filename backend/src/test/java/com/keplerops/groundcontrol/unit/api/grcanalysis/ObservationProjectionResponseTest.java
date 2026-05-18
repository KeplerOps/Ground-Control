package com.keplerops.groundcontrol.unit.api.grcanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.api.grcanalysis.ObservationProjectionResponse;
import com.keplerops.groundcontrol.domain.grcanalysis.service.ObservationProjectionMode;
import com.keplerops.groundcontrol.domain.grcanalysis.service.ObservationProjectionResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pure mapping test for the ObservationProjection API DTO. Covers both modes
 * (ASSET_EXPOSURE populates assetExposures + currentObservations; CONTROL_STATE
 * populates controlStates) plus the empty-list branch.
 */
class ObservationProjectionResponseTest {

    @Test
    void from_assetExposureMode_mapsAllAssetExposureFields() {
        UUID assetId = UUID.randomUUID();
        UUID observationId = UUID.randomUUID();
        UUID inputsAssetId = UUID.randomUUID();
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        Instant observedAt = asOf.minusSeconds(86400L * 2);
        Instant expiresAt = asOf.plusSeconds(86400L * 30);

        ObservationProjectionResult.CurrentObservation current = new ObservationProjectionResult.CurrentObservation(
                observationId, "CONFIGURATION", "patch-level", observedAt, expiresAt, "CURRENT");
        ObservationProjectionResult.AssetExposureItem exposure = new ObservationProjectionResult.AssetExposureItem(
                assetId, "ASSET-1", "APPLICATION", "CURRENT", List.of(current));
        ObservationProjectionResult result = new ObservationProjectionResult(
                "observation_exposure",
                "ground-control",
                asOf,
                "observation-current-state-projection-v1",
                new ObservationProjectionResult.Inputs(
                        "ground-control", asOf, ObservationProjectionMode.ASSET_EXPOSURE, inputsAssetId, null),
                List.of(exposure),
                List.of(),
                List.of("ignored-controlId-limitation"));

        ObservationProjectionResponse response = ObservationProjectionResponse.from(result);

        assertThat(response.analysisKind()).isEqualTo("observation_exposure");
        assertThat(response.project()).isEqualTo("ground-control");
        assertThat(response.asOf()).isEqualTo(asOf);
        assertThat(response.derivationMethod()).isEqualTo("observation-current-state-projection-v1");

        ObservationProjectionResponse.Inputs mappedInputs = response.inputs();
        assertThat(mappedInputs.project()).isEqualTo("ground-control");
        assertThat(mappedInputs.asOf()).isEqualTo(asOf);
        assertThat(mappedInputs.mode()).isEqualTo(ObservationProjectionMode.ASSET_EXPOSURE);
        assertThat(mappedInputs.assetId()).isEqualTo(inputsAssetId);
        assertThat(mappedInputs.controlId()).isNull();

        assertThat(response.assetExposures()).hasSize(1);
        ObservationProjectionResponse.AssetExposureItem mappedExposure =
                response.assetExposures().get(0);
        assertThat(mappedExposure.assetId()).isEqualTo(assetId);
        assertThat(mappedExposure.assetUid()).isEqualTo("ASSET-1");
        assertThat(mappedExposure.assetType()).isEqualTo("APPLICATION");
        assertThat(mappedExposure.state()).isEqualTo("CURRENT");

        assertThat(mappedExposure.currentObservations()).hasSize(1);
        ObservationProjectionResponse.CurrentObservation mappedCurrent =
                mappedExposure.currentObservations().get(0);
        assertThat(mappedCurrent.id()).isEqualTo(observationId);
        assertThat(mappedCurrent.category()).isEqualTo("CONFIGURATION");
        assertThat(mappedCurrent.observationKey()).isEqualTo("patch-level");
        assertThat(mappedCurrent.observedAt()).isEqualTo(observedAt);
        assertThat(mappedCurrent.expiresAt()).isEqualTo(expiresAt);
        assertThat(mappedCurrent.state()).isEqualTo("CURRENT");

        assertThat(response.controlStates()).isEmpty();
        assertThat(response.limitations()).containsExactly("ignored-controlId-limitation");
    }

    @Test
    void from_controlStateMode_mapsAllControlStateFields() {
        UUID controlId = UUID.randomUUID();
        UUID inputsControlId = UUID.randomUUID();
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        LocalDate assessedAt = LocalDate.of(2026, 4, 1);

        ObservationProjectionResult.ControlStateItem controlState = new ObservationProjectionResult.ControlStateItem(
                controlId, "CTRL-1", "OPERATIONAL", "EFFECTIVE", "PARTIALLY_EFFECTIVE", assessedAt, "CURRENT");
        ObservationProjectionResult result = new ObservationProjectionResult(
                "control_state",
                "ground-control",
                asOf,
                "observation-current-state-projection-v1",
                new ObservationProjectionResult.Inputs(
                        "ground-control", asOf, ObservationProjectionMode.CONTROL_STATE, null, inputsControlId),
                List.of(),
                List.of(controlState),
                List.of("ControlStatus.OPERATIONAL is NOT treated as evidence"));

        ObservationProjectionResponse response = ObservationProjectionResponse.from(result);

        assertThat(response.analysisKind()).isEqualTo("control_state");
        assertThat(response.inputs().mode()).isEqualTo(ObservationProjectionMode.CONTROL_STATE);
        assertThat(response.inputs().controlId()).isEqualTo(inputsControlId);
        assertThat(response.assetExposures()).isEmpty();
        assertThat(response.controlStates()).hasSize(1);

        ObservationProjectionResponse.ControlStateItem mapped =
                response.controlStates().get(0);
        assertThat(mapped.controlId()).isEqualTo(controlId);
        assertThat(mapped.controlUid()).isEqualTo("CTRL-1");
        assertThat(mapped.controlStatus()).isEqualTo("OPERATIONAL");
        assertThat(mapped.designEffectiveness()).isEqualTo("EFFECTIVE");
        assertThat(mapped.operatingEffectiveness()).isEqualTo("PARTIALLY_EFFECTIVE");
        assertThat(mapped.latestAssessedAt()).isEqualTo(assessedAt);
        assertThat(mapped.state()).isEqualTo("CURRENT");

        assertThat(response.limitations()).containsExactly("ControlStatus.OPERATIONAL is NOT treated as evidence");
    }

    @Test
    void from_emptyListsAndNullFields_mapCleanly() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        ObservationProjectionResult result = new ObservationProjectionResult(
                "observation_exposure",
                "ground-control",
                asOf,
                "observation-current-state-projection-v1",
                new ObservationProjectionResult.Inputs(
                        "ground-control", asOf, ObservationProjectionMode.ASSET_EXPOSURE, null, null),
                List.of(),
                List.of(),
                List.of());

        ObservationProjectionResponse response = ObservationProjectionResponse.from(result);

        assertThat(response.assetExposures()).isEmpty();
        assertThat(response.controlStates()).isEmpty();
        assertThat(response.limitations()).isEmpty();
        assertThat(response.inputs().assetId()).isNull();
        assertThat(response.inputs().controlId()).isNull();
    }
}
