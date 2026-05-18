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
 * preserves every field on the wire. Split into one test per nested record so
 * each method stays under the Sonar S5961 assertion threshold (25) without
 * losing the field-level coverage.
 */
class EvidenceFreshnessResponseTest {

    private static final Instant AS_OF = Instant.parse("2026-05-18T00:00:00Z");
    private static final Instant DERIVED_AT = AS_OF.minusSeconds(86400L * 7);
    private static final Instant OBSERVED_AT = AS_OF.minusSeconds(86400L * 3);
    private static final Instant EXPIRES_AT = AS_OF.plusSeconds(86400L * 30);
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 4, 12);
    private static final UUID ARTIFACT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SUPERSEDED_BY_ID = UUID.fromString("11111111-1111-1111-1111-111111111112");
    private static final UUID OBSERVATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222221");
    private static final UUID ASSET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CONTROL_TEST_ID = UUID.fromString("33333333-3333-3333-3333-333333333331");
    private static final UUID CONTROL_ID = UUID.fromString("33333333-3333-3333-3333-333333333332");
    private static final UUID INPUTS_ASSET_ID = UUID.fromString("44444444-4444-4444-4444-444444444441");
    private static final UUID INPUTS_CONTROL_ID = UUID.fromString("44444444-4444-4444-4444-444444444442");

    private static EvidenceFreshnessResult populatedResult() {
        EvidenceFreshnessResult.Inputs inputs = new EvidenceFreshnessResult.Inputs(
                "ground-control", AS_OF, 90, true, INPUTS_ASSET_ID, INPUTS_CONTROL_ID);
        EvidenceFreshnessResult.EvidenceArtifactFreshnessItem artifact =
                new EvidenceFreshnessResult.EvidenceArtifactFreshnessItem(
                        ARTIFACT_ID, "EVD-1", "Artifact title", DERIVED_AT, 7L, "FRESH", SUPERSEDED_BY_ID);
        EvidenceFreshnessResult.ObservationFreshnessItem observation =
                new EvidenceFreshnessResult.ObservationFreshnessItem(
                        OBSERVATION_ID,
                        ASSET_ID,
                        "ASSET-1",
                        "CONFIGURATION",
                        "patch-level",
                        OBSERVED_AT,
                        EXPIRES_AT,
                        3L,
                        "FRESH");
        EvidenceFreshnessResult.ControlTestFreshnessItem controlTest =
                new EvidenceFreshnessResult.ControlTestFreshnessItem(
                        CONTROL_TEST_ID, "CT-1", CONTROL_ID, "CTRL-1", TEST_DATE, 36L, "FRESH");
        EvidenceFreshnessResult.EvidenceFreshnessCounts counts =
                new EvidenceFreshnessResult.EvidenceFreshnessCounts(2, 1, 1, 3, 3);
        return new EvidenceFreshnessResult(
                "evidence_freshness",
                "ground-control",
                AS_OF,
                "evidence-freshness-projection-v1",
                inputs,
                List.of(artifact),
                List.of(observation),
                List.of(controlTest),
                counts,
                List.of("limitation-a", "limitation-b"));
    }

    @Test
    void from_topLevel_mapsAnalysisKindProjectAsOfDerivationMethod() {
        EvidenceFreshnessResponse response = EvidenceFreshnessResponse.from(populatedResult());

        assertThat(response.analysisKind()).isEqualTo("evidence_freshness");
        assertThat(response.project()).isEqualTo("ground-control");
        assertThat(response.asOf()).isEqualTo(AS_OF);
        assertThat(response.derivationMethod()).isEqualTo("evidence-freshness-projection-v1");
    }

    @Test
    void from_inputs_mapsEveryField() {
        EvidenceFreshnessResponse.Inputs mappedInputs =
                EvidenceFreshnessResponse.from(populatedResult()).inputs();

        assertThat(mappedInputs.project()).isEqualTo("ground-control");
        assertThat(mappedInputs.asOf()).isEqualTo(AS_OF);
        assertThat(mappedInputs.freshnessWindowDays()).isEqualTo(90);
        assertThat(mappedInputs.includeSuperseded()).isTrue();
        assertThat(mappedInputs.assetId()).isEqualTo(INPUTS_ASSET_ID);
        assertThat(mappedInputs.controlId()).isEqualTo(INPUTS_CONTROL_ID);
    }

    @Test
    void from_evidenceArtifactItem_mapsEveryField() {
        EvidenceFreshnessResponse response = EvidenceFreshnessResponse.from(populatedResult());

        assertThat(response.evidenceArtifacts()).hasSize(1);
        EvidenceFreshnessResponse.EvidenceArtifactFreshnessItem mappedArtifact =
                response.evidenceArtifacts().get(0);
        assertThat(mappedArtifact.id()).isEqualTo(ARTIFACT_ID);
        assertThat(mappedArtifact.uid()).isEqualTo("EVD-1");
        assertThat(mappedArtifact.title()).isEqualTo("Artifact title");
        assertThat(mappedArtifact.derivedAt()).isEqualTo(DERIVED_AT);
        assertThat(mappedArtifact.ageDays()).isEqualTo(7L);
        assertThat(mappedArtifact.state()).isEqualTo("FRESH");
        assertThat(mappedArtifact.supersededByArtifactId()).isEqualTo(SUPERSEDED_BY_ID);
    }

    @Test
    void from_observationItem_mapsEveryField() {
        EvidenceFreshnessResponse response = EvidenceFreshnessResponse.from(populatedResult());

        assertThat(response.observations()).hasSize(1);
        EvidenceFreshnessResponse.ObservationFreshnessItem mappedObservation =
                response.observations().get(0);
        assertThat(mappedObservation.id()).isEqualTo(OBSERVATION_ID);
        assertThat(mappedObservation.assetId()).isEqualTo(ASSET_ID);
        assertThat(mappedObservation.assetUid()).isEqualTo("ASSET-1");
        assertThat(mappedObservation.category()).isEqualTo("CONFIGURATION");
        assertThat(mappedObservation.observationKey()).isEqualTo("patch-level");
        assertThat(mappedObservation.observedAt()).isEqualTo(OBSERVED_AT);
        assertThat(mappedObservation.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(mappedObservation.ageDays()).isEqualTo(3L);
        assertThat(mappedObservation.state()).isEqualTo("FRESH");
    }

    @Test
    void from_controlTestItem_mapsEveryField() {
        EvidenceFreshnessResponse response = EvidenceFreshnessResponse.from(populatedResult());

        assertThat(response.controlTests()).hasSize(1);
        EvidenceFreshnessResponse.ControlTestFreshnessItem mappedTest =
                response.controlTests().get(0);
        assertThat(mappedTest.id()).isEqualTo(CONTROL_TEST_ID);
        assertThat(mappedTest.uid()).isEqualTo("CT-1");
        assertThat(mappedTest.controlId()).isEqualTo(CONTROL_ID);
        assertThat(mappedTest.controlUid()).isEqualTo("CTRL-1");
        assertThat(mappedTest.testDate()).isEqualTo(TEST_DATE);
        assertThat(mappedTest.ageDays()).isEqualTo(36L);
        assertThat(mappedTest.state()).isEqualTo("FRESH");
    }

    @Test
    void from_countsAndLimitations_mapEveryField() {
        EvidenceFreshnessResponse response = EvidenceFreshnessResponse.from(populatedResult());

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
        EvidenceFreshnessResult result = new EvidenceFreshnessResult(
                "evidence_freshness",
                "ground-control",
                AS_OF,
                "evidence-freshness-projection-v1",
                new EvidenceFreshnessResult.Inputs("ground-control", AS_OF, 90, false, null, null),
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
