package com.keplerops.groundcontrol.unit.domain.grcanalysis;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlTest;
import com.keplerops.groundcontrol.domain.controls.repository.ControlEffectivenessAssessmentRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlTestRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion;
import com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology;
import com.keplerops.groundcontrol.domain.evidence.model.EvidenceArtifact;
import com.keplerops.groundcontrol.domain.evidence.model.EvidenceSourceRef;
import com.keplerops.groundcontrol.domain.evidence.repository.EvidenceArtifactRepository;
import com.keplerops.groundcontrol.domain.evidence.state.EvidenceSourceKind;
import com.keplerops.groundcontrol.domain.evidence.state.EvidenceType;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.grcanalysis.service.EvidenceFreshnessAnalysisService;
import com.keplerops.groundcontrol.domain.grcanalysis.service.EvidenceFreshnessResult;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvidenceFreshnessAnalysisServiceTest {

    @Mock
    private EvidenceArtifactRepository evidenceArtifactRepository;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private ControlTestRepository controlTestRepository;

    @Mock
    private ControlEffectivenessAssessmentRepository controlEffectivenessAssessmentRepository;

    @Mock
    private OperationalAssetRepository operationalAssetRepository;

    @Mock
    private AssetLinkRepository assetLinkRepository;

    @Mock
    private ControlLinkRepository controlLinkRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private EvidenceFreshnessAnalysisService service;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private EvidenceArtifact makeArtifact(String uid, Instant derivedAt, UUID supersededBy) {
        var ea = new EvidenceArtifact(
                project,
                uid,
                "Title " + uid,
                "Summary " + uid,
                EvidenceType.ASSURANCE_CONCLUSION,
                "manual-rollup-v1",
                derivedAt);
        setField(ea, "id", UUID.randomUUID());
        if (supersededBy != null) {
            ea.setSupersededByArtifactId(supersededBy);
        }
        return ea;
    }

    private Observation makeObservation(OperationalAsset asset, Instant observedAt, Instant expiresAt) {
        var obs = new Observation(
                asset, ObservationCategory.CONFIGURATION, "patch-level", "1.0", "scanner-x", observedAt);
        setField(obs, "id", UUID.randomUUID());
        if (expiresAt != null) {
            obs.setExpiresAt(expiresAt);
        }
        return obs;
    }

    private OperationalAsset makeAsset(String uid) {
        var asset = new OperationalAsset(project, uid, "Asset " + uid);
        setField(asset, "id", UUID.randomUUID());
        return asset;
    }

    private ControlTest makeControlTest(String uid, LocalDate testDate) {
        var control = new Control(project, "CTRL-1", "Control 1", ControlFunction.DETECTIVE);
        setField(control, "id", UUID.randomUUID());
        var ct = new ControlTest(
                project,
                control,
                uid,
                ControlTestMethodology.INSPECTION,
                ControlTestConclusion.EFFECTIVE,
                "tester@example.com",
                testDate);
        setField(ct, "id", UUID.randomUUID());
        return ct;
    }

    @Test
    void happyPath_returnsStructuredResultWithAllRequiredFields() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var fresh = makeArtifact("EVD-1", asOf.minusSeconds(86400L * 10), null); // 10 days old
        var stale = makeArtifact("EVD-2", asOf.minusSeconds(86400L * 200), null); // 200 days old
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(projectId, asOf))
                .thenReturn(List.of(fresh, stale));
        when(observationRepository.findByProjectIdAndObservedAtLessThanEqual(projectId, asOf))
                .thenReturn(List.of());
        when(controlTestRepository.findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(
                        eq(projectId), any(LocalDate.class)))
                .thenReturn(List.of());

        EvidenceFreshnessResult result = service.analyze(projectId, asOf, 90, false, null, null);

        assertThat(result.analysisKind()).isEqualTo("evidence_freshness");
        assertThat(result.project()).isEqualTo("ground-control");
        assertThat(result.asOf()).isEqualTo(asOf);
        assertThat(result.derivationMethod()).isEqualTo("evidence-freshness-projection-v1");
        assertThat(result.inputs().freshnessWindowDays()).isEqualTo(90);
        assertThat(result.inputs().includeSuperseded()).isFalse();
        assertThat(result.evidenceArtifacts()).hasSize(2);
        assertThat(result.evidenceArtifacts())
                .extracting(EvidenceFreshnessResult.EvidenceArtifactFreshnessItem::state)
                .containsExactly("FRESH", "STALE");
        assertThat(result.counts().fresh()).isEqualTo(1);
        assertThat(result.counts().stale()).isEqualTo(1);
        assertThat(result.counts().currentlyValid()).isEqualTo(2);
        assertThat(result.limitations()).isNotNull();
    }

    @Test
    void timeTravelAsOf_changesFreshnessState() {
        Instant artifactDerivedAt = Instant.parse("2026-01-01T00:00:00Z");
        var artifact = makeArtifact("EVD-1", artifactDerivedAt, null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(
                        eq(projectId), any(Instant.class)))
                .thenReturn(List.of(artifact));
        when(observationRepository.findByProjectIdAndObservedAtLessThanEqual(eq(projectId), any(Instant.class)))
                .thenReturn(List.of());
        when(controlTestRepository.findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(
                        eq(projectId), any(LocalDate.class)))
                .thenReturn(List.of());

        // 30 days later - within 90-day window - FRESH
        var nearResult = service.analyze(projectId, artifactDerivedAt.plusSeconds(86400L * 30), 90, false, null, null);
        assertThat(nearResult.evidenceArtifacts().get(0).state()).isEqualTo("FRESH");

        // 120 days later - outside window - STALE
        var farResult = service.analyze(projectId, artifactDerivedAt.plusSeconds(86400L * 120), 90, false, null, null);
        assertThat(farResult.evidenceArtifacts().get(0).state()).isEqualTo("STALE");
    }

    /** Finding #2: a row with {@code derivedAt > asOf} must be excluded. */
    @Test
    void asOf_excludesFutureArtifacts() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var future = makeArtifact("EVD-FUTURE", asOf.plusSeconds(86400L), null);
        // The repository method bounds derivedAt <= asOf, so simulate the repo
        // correctly filtering and verify the service does not need to filter
        // again in-memory while still returning a clean projection.
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(projectId, asOf))
                .thenReturn(List.of());
        when(observationRepository.findByProjectIdAndObservedAtLessThanEqual(projectId, asOf))
                .thenReturn(List.of());
        when(controlTestRepository.findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(
                        eq(projectId), any(LocalDate.class)))
                .thenReturn(List.of());

        EvidenceFreshnessResult result = service.analyze(projectId, asOf, 90, false, null, null);

        assertThat(result.evidenceArtifacts()).isEmpty();
        // The unscoped (no-filter) repository must not have been hit; only the
        // asOf-bounded one.
        verify(evidenceArtifactRepository, never()).findByProjectIdOrderByDerivedAtDesc(any());
        // Force the test object to be in scope so the future artifact is not
        // silently DCE'd by the compiler in a future refactor.
        assertThat(future.getDerivedAt()).isAfter(asOf);
    }

    @Test
    void expiredObservation_returnsExpiredState() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var asset = makeAsset("ASSET-1");
        var expiredObs = makeObservation(asset, asOf.minusSeconds(86400L * 30), asOf.minusSeconds(86400L * 1));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(projectId, asOf))
                .thenReturn(List.of());
        when(observationRepository.findByProjectIdAndObservedAtLessThanEqual(projectId, asOf))
                .thenReturn(List.of(expiredObs));
        when(controlTestRepository.findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(
                        eq(projectId), any(LocalDate.class)))
                .thenReturn(List.of());

        EvidenceFreshnessResult result = service.analyze(projectId, asOf, 90, false, null, null);

        assertThat(result.observations()).hasSize(1);
        assertThat(result.observations().get(0).state()).isEqualTo("EXPIRED");
        assertThat(result.counts().expired()).isEqualTo(1);
    }

    @Test
    void supersededArtifact_excludedWhenFlagFalse_includedWhenTrue() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var superseded = makeArtifact("EVD-OLD", asOf.minusSeconds(86400L * 10), UUID.randomUUID());
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(projectId, asOf))
                .thenReturn(List.of(superseded));
        when(observationRepository.findByProjectIdAndObservedAtLessThanEqual(projectId, asOf))
                .thenReturn(List.of());
        when(controlTestRepository.findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(
                        eq(projectId), any(LocalDate.class)))
                .thenReturn(List.of());

        var excluded = service.analyze(projectId, asOf, 90, false, null, null);
        assertThat(excluded.evidenceArtifacts()).isEmpty();
        assertThat(excluded.limitations()).anyMatch(s -> s.contains("supersededByArtifactId"));

        var included = service.analyze(projectId, asOf, 90, true, null, null);
        assertThat(included.evidenceArtifacts()).hasSize(1);
        assertThat(included.evidenceArtifacts().get(0).state()).isEqualTo("SUPERSEDED");
        assertThat(included.counts().superseded()).isEqualTo(1);
    }

    @Test
    void assetFilter_excludesArtifactsWithoutObservationSourcesOnAsset() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var asset = makeAsset("ASSET-1");
        var obs = makeObservation(asset, asOf.minusSeconds(86400L * 5), null);
        var artifactWithObsRef = makeArtifact("EVD-1", asOf.minusSeconds(86400L * 1), null);
        artifactWithObsRef.setSources(
                List.of(new EvidenceSourceRef(EvidenceSourceKind.OBSERVATION, obs.getId(), null, "primary")));
        var artifactNoObsRef = makeArtifact("EVD-2", asOf.minusSeconds(86400L * 1), null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByIdAndProjectId(asset.getId(), projectId))
                .thenReturn(Optional.of(asset));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(projectId, asOf))
                .thenReturn(List.of(artifactWithObsRef, artifactNoObsRef));
        when(observationRepository.findByAssetIdAndObservedAtLessThanEqual(asset.getId(), asOf))
                .thenReturn(List.of(obs));
        when(assetLinkRepository.findByAssetIdAndTargetType(asset.getId(), AssetLinkTargetType.CONTROL))
                .thenReturn(List.of());
        when(controlLinkRepository.findByProjectId(projectId)).thenReturn(List.of());

        EvidenceFreshnessResult result = service.analyze(projectId, asOf, 90, false, asset.getId(), null);

        assertThat(result.evidenceArtifacts()).hasSize(1);
        assertThat(result.evidenceArtifacts().get(0).uid()).isEqualTo("EVD-1");
        assertThat(result.limitations()).anyMatch(s -> s.contains("asset filter"));
    }

    /** Finding #1: a cross-project assetId must be rejected with NotFoundException. */
    @Test
    void crossProjectAssetId_isRejectedAsNotFound() {
        UUID foreignAssetId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByIdAndProjectId(foreignAssetId, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyze(
                        projectId, Instant.parse("2026-05-18T00:00:00Z"), 90, false, foreignAssetId, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Asset not found in project");
        // The asset-scoped observation lookup must not even be issued.
        verify(observationRepository, never()).findByAssetIdAndObservedAtLessThanEqual(eq(foreignAssetId), any());
        verify(observationRepository, never()).findByAssetId(foreignAssetId);
    }

    @Test
    void controlTestProjection_oldTestIsStale() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var oldTest = makeControlTest("CT-1", LocalDate.of(2025, 1, 1)); // way more than 90 days
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(projectId, asOf))
                .thenReturn(List.of());
        when(observationRepository.findByProjectIdAndObservedAtLessThanEqual(projectId, asOf))
                .thenReturn(List.of());
        when(controlTestRepository.findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(
                        eq(projectId), any(LocalDate.class)))
                .thenReturn(List.of(oldTest));

        EvidenceFreshnessResult result = service.analyze(projectId, asOf, 90, false, null, null);

        assertThat(result.controlTests()).hasSize(1);
        assertThat(result.controlTests().get(0).state()).isEqualTo("STALE");
    }

    /**
     * Finding #3: a controlId-only filter must narrow observations rather than
     * returning the entire project's observation list. With no Control->Asset
     * model linkage, observations is empty plus a limitation.
     */
    @Test
    void controlIdOnly_observationsSectionIsEmpty_withLimitation() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        UUID controlId = UUID.randomUUID();
        var asset = makeAsset("ASSET-1");
        var rogue = makeObservation(asset, asOf.minusSeconds(86400L * 5), null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(projectId, asOf))
                .thenReturn(List.of());
        when(observationRepository.findByProjectIdAndObservedAtLessThanEqual(projectId, asOf))
                .thenReturn(List.of(rogue));
        when(controlTestRepository.findByProjectIdAndControlIdAndTestDateLessThanEqualOrderByTestDateDesc(
                        eq(projectId), eq(controlId), any(LocalDate.class)))
                .thenReturn(List.of());
        when(controlEffectivenessAssessmentRepository.findByProjectIdAndControlIdOrderByAssessedAtDesc(
                        projectId, controlId))
                .thenReturn(List.of());

        EvidenceFreshnessResult result = service.analyze(projectId, asOf, 90, false, null, controlId);

        assertThat(result.observations()).isEmpty();
        assertThat(result.limitations()).anyMatch(s -> s.contains("observations narrowed by controlId"));
    }

    /**
     * Finding #3: an assetId-only filter must narrow control-tests; when no
     * Asset->Control link edge resolves, control-tests is empty plus a
     * limitation explaining the carve-out.
     */
    @Test
    void assetIdOnly_controlTestsEmpty_whenNoAssetControlLinkage() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var asset = makeAsset("ASSET-1");
        var rogueTest = makeControlTest("CT-1", LocalDate.of(2025, 1, 1));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByIdAndProjectId(asset.getId(), projectId))
                .thenReturn(Optional.of(asset));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(projectId, asOf))
                .thenReturn(List.of());
        when(observationRepository.findByAssetIdAndObservedAtLessThanEqual(asset.getId(), asOf))
                .thenReturn(List.of());
        when(controlTestRepository.findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(
                        eq(projectId), any(LocalDate.class)))
                .thenReturn(List.of(rogueTest));
        when(assetLinkRepository.findByAssetIdAndTargetType(asset.getId(), AssetLinkTargetType.CONTROL))
                .thenReturn(List.of());
        when(controlLinkRepository.findByProjectId(projectId)).thenReturn(List.of());

        EvidenceFreshnessResult result = service.analyze(projectId, asOf, 90, false, asset.getId(), null);

        assertThat(result.controlTests()).isEmpty();
        assertThat(result.limitations()).anyMatch(s -> s.contains("control-tests narrowed by assetId"));
    }

    /**
     * Finding #4: an artifact with a CEA source whose assessment is for a
     * different control must NOT match when controlId narrows the artifact set.
     */
    @Test
    void controlIdFilter_artifactWithUnrelatedCeaSource_isExcluded() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        UUID controlId = UUID.randomUUID();
        UUID unrelatedCeaId = UUID.randomUUID();
        var artifact = makeArtifact("EVD-1", asOf.minusSeconds(86400L * 1), null);
        artifact.setSources(List.of(new EvidenceSourceRef(
                EvidenceSourceKind.CONTROL_EFFECTIVENESS_ASSESSMENT, unrelatedCeaId, null, "primary")));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(projectId, asOf))
                .thenReturn(List.of(artifact));
        when(controlTestRepository.findByProjectIdAndControlIdAndTestDateLessThanEqualOrderByTestDateDesc(
                        eq(projectId), eq(controlId), any(LocalDate.class)))
                .thenReturn(List.of());
        when(controlEffectivenessAssessmentRepository.findByProjectIdAndControlIdOrderByAssessedAtDesc(
                        projectId, controlId))
                .thenReturn(List.of()); // no CEAs for this control: unrelatedCeaId must NOT match
        when(observationRepository.findByProjectIdAndObservedAtLessThanEqual(projectId, asOf))
                .thenReturn(List.of());

        EvidenceFreshnessResult result = service.analyze(projectId, asOf, 90, false, null, controlId);

        assertThat(result.evidenceArtifacts()).isEmpty();
    }

    @Test
    void projectNotFound_throws() {
        when(projectRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyze(projectId, Instant.now(), 90, false, null, null))
                .isInstanceOf(NotFoundException.class);
    }

    /** Finding #8: non-positive freshnessWindowDays must throw at the service boundary. */
    @Test
    void invalidFreshnessWindow_throwsDomainValidationException() {
        assertThatThrownBy(() -> service.analyze(projectId, Instant.now(), 0, false, null, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.analyze(projectId, Instant.now(), -30, false, null, null))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void asOfDefaultsToNow_whenNull() {
        var fresh = makeArtifact("EVD-1", Instant.now().minusSeconds(86400L * 5), null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(
                        eq(projectId), any(Instant.class)))
                .thenReturn(List.of(fresh));
        when(observationRepository.findByProjectIdAndObservedAtLessThanEqual(eq(projectId), any(Instant.class)))
                .thenReturn(List.of());
        when(controlTestRepository.findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(
                        eq(projectId), any(LocalDate.class)))
                .thenReturn(List.of());

        Instant before = Instant.now();
        EvidenceFreshnessResult result = service.analyze(projectId, null, 90, false, null, null);
        Instant after = Instant.now();

        assertThat(result.inputs().freshnessWindowDays()).isEqualTo(90);
        // asOf must default to ~now(), not Instant.EPOCH/MIN/some other constant.
        // Use a wall-clock window bracketing the service call so we catch wrong
        // epochs (which would silently break freshness math for every item).
        assertThat(result.asOf()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }
}
