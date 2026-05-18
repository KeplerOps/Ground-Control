package com.keplerops.groundcontrol.unit.api.grcanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.api.grcanalysis.EvidenceFreshnessResponse;
import com.keplerops.groundcontrol.domain.grcanalysis.service.EvidenceFreshnessResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pure mapping test for the EvidenceFreshness API DTO. Builds a fully
 * populated domain result and asserts that every nested record's {@code from()}
 * preserves every field on the wire.
 */
class EvidenceFreshnessResponseTest {

    @Test
    void from_mapsAllFieldsAcrossEveryNestedRecord() {
        UUID artifactId = UUID.randomUUID();
        UUID supersededById = UUID.randomUUID();
        UUID observationId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID controlTestId = UUID.randomUUID();
        UUID controlId = UUID.randomUUID();
        UUID inputsAssetId = UUID.randomUUID();
        UUID inputsControlId = UUID.randomUUID();
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        Instant derivedAt = asOf.minusSeconds(86400L * 7);
        Instant observedAt = asOf.minusSeconds(86400L * 3);
        Instant expiresAt = asOf.plusSeconds(86400L * 30);
        LocalDate testDate = LocalDate.of(2026, 4, 12);

        EvidenceFreshnessResult.Inputs inputs =
                new EvidenceFreshnessResult.Inputs("ground-control", asOf, 90, true, inputsAssetId, inputsControlId);
        EvidenceFreshnessResult.EvidenceArtifactFreshnessItem artifact =
                new EvidenceFreshnessResult.EvidenceArtifactFreshnessItem(
                        artifactId, "EVD-1", "Artifact title", derivedAt, 7L, "FRESH", supersededById);
        EvidenceFreshnessResult.ObservationFreshnessItem observation =
                new EvidenceFreshnessResult.ObservationFreshnessItem(
                        observationId,
                        assetId,
                        "ASSET-1",
                        "CONFIGURATION",
                        "patch-level",
                        observedAt,
                        expiresAt,
                        3L,
                        "FRESH");
        EvidenceFreshnessResult.ControlTestFreshnessItem controlTest =
                new EvidenceFreshnessResult.ControlTestFreshnessItem(
                        controlTestId, "CT-1", controlId, "CTRL-1", testDate, 36L, "FRESH");
        EvidenceFreshnessResult.EvidenceFreshnessCounts counts =
                new EvidenceFreshnessResult.EvidenceFreshnessCounts(2, 1, 1, 3, 3);
        EvidenceFreshnessResult result = new EvidenceFreshnessResult(
                "evidence_freshness",
                "ground-control",
                asOf,
                "evidence-freshness-projection-v1",
                inputs,
                List.of(artifact),
                List.of(observation),
                List.of(controlTest),
                counts,
                List.of("limitation-a", "limitation-b"));

        EvidenceFreshnessResponse response = EvidenceFreshnessResponse.from(result);

        assertThat(response.analysisKind()).isEqualTo("evidence_freshness");
        assertThat(response.project()).isEqualTo("ground-control");
        assertThat(response.asOf()).isEqualTo(asOf);
        assertThat(response.derivationMethod()).isEqualTo("evidence-freshness-projection-v1");

        EvidenceFreshnessResponse.Inputs mappedInputs = response.inputs();
        assertThat(mappedInputs.project()).isEqualTo("ground-control");
        assertThat(mappedInputs.asOf()).isEqualTo(asOf);
        assertThat(mappedInputs.freshnessWindowDays()).isEqualTo(90);
        assertThat(mappedInputs.includeSuperseded()).isTrue();
        assertThat(mappedInputs.assetId()).isEqualTo(inputsAssetId);
        assertThat(mappedInputs.controlId()).isEqualTo(inputsControlId);

        assertThat(response.evidenceArtifacts()).hasSize(1);
        EvidenceFreshnessResponse.EvidenceArtifactFreshnessItem mappedArtifact =
                response.evidenceArtifacts().get(0);
        assertThat(mappedArtifact.id()).isEqualTo(artifactId);
        assertThat(mappedArtifact.uid()).isEqualTo("EVD-1");
        assertThat(mappedArtifact.title()).isEqualTo("Artifact title");
        assertThat(mappedArtifact.derivedAt()).isEqualTo(derivedAt);
        assertThat(mappedArtifact.ageDays()).isEqualTo(7L);
        assertThat(mappedArtifact.state()).isEqualTo("FRESH");
        assertThat(mappedArtifact.supersededByArtifactId()).isEqualTo(supersededById);

        assertThat(response.observations()).hasSize(1);
        EvidenceFreshnessResponse.ObservationFreshnessItem mappedObservation =
                response.observations().get(0);
        assertThat(mappedObservation.id()).isEqualTo(observationId);
        assertThat(mappedObservation.assetId()).isEqualTo(assetId);
        assertThat(mappedObservation.assetUid()).isEqualTo("ASSET-1");
        assertThat(mappedObservation.category()).isEqualTo("CONFIGURATION");
        assertThat(mappedObservation.observationKey()).isEqualTo("patch-level");
        assertThat(mappedObservation.observedAt()).isEqualTo(observedAt);
        assertThat(mappedObservation.expiresAt()).isEqualTo(expiresAt);
        assertThat(mappedObservation.ageDays()).isEqualTo(3L);
        assertThat(mappedObservation.state()).isEqualTo("FRESH");

        assertThat(response.controlTests()).hasSize(1);
        EvidenceFreshnessResponse.ControlTestFreshnessItem mappedTest =
                response.controlTests().get(0);
        assertThat(mappedTest.id()).isEqualTo(controlTestId);
        assertThat(mappedTest.uid()).isEqualTo("CT-1");
        assertThat(mappedTest.controlId()).isEqualTo(controlId);
        assertThat(mappedTest.controlUid()).isEqualTo("CTRL-1");
        assertThat(mappedTest.testDate()).isEqualTo(testDate);
        assertThat(mappedTest.ageDays()).isEqualTo(36L);
        assertThat(mappedTest.state()).isEqualTo("FRESH");

        EvidenceFreshnessResponse.EvidenceFreshnessCounts mappedCounts = response.counts();
        assertThat(mappedCounts.fresh()).isEqualTo(2);
        assertThat(mappedCounts.stale()).isEqualTo(1);
        assertThat(mappedCounts.expired()).isEqualTo(1);
        assertThat(mappedCounts.superseded()).isEqualTo(3);
        assertThat(mappedCounts.currentlyValid()).isEqualTo(3);

        assertThat(response.limitations()).containsExactly("limitation-a", "limitation-b");
    }

    @Test
    void from_emptyListsAndZeroCounts_mapCleanly() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        EvidenceFreshnessResult result = new EvidenceFreshnessResult(
                "evidence_freshness",
                "ground-control",
                asOf,
                "evidence-freshness-projection-v1",
                new EvidenceFreshnessResult.Inputs("ground-control", asOf, 90, false, null, null),
                List.of(),
                List.of(),
                List.of(),
                new EvidenceFreshnessResult.EvidenceFreshnessCounts(0, 0, 0, 0, 0),
                List.of());

        EvidenceFreshnessResponse response = EvidenceFreshnessResponse.from(result);

        assertThat(response.evidenceArtifacts()).isEmpty();
        assertThat(response.observations()).isEmpty();
        assertThat(response.controlTests()).isEmpty();
        assertThat(response.limitations()).isEmpty();
        assertThat(response.counts().fresh()).isZero();
        assertThat(response.counts().stale()).isZero();
        assertThat(response.counts().expired()).isZero();
        assertThat(response.counts().superseded()).isZero();
        assertThat(response.counts().currentlyValid()).isZero();
        assertThat(response.inputs().assetId()).isNull();
        assertThat(response.inputs().controlId()).isNull();
        assertThat(response.inputs().includeSuperseded()).isFalse();
    }
}
