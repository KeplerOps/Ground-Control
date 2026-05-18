package com.keplerops.groundcontrol.unit.domain.grcanalysis;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.assets.state.ObservationCategory;
import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment;
import com.keplerops.groundcontrol.domain.controls.repository.ControlEffectivenessAssessmentRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlEffectivenessRating;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlStatus;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.grcanalysis.service.ObservationProjectionMode;
import com.keplerops.groundcontrol.domain.grcanalysis.service.ObservationProjectionResult;
import com.keplerops.groundcontrol.domain.grcanalysis.service.ObservationProjectionService;
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
class ObservationProjectionServiceTest {

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private OperationalAssetRepository operationalAssetRepository;

    @Mock
    private ControlRepository controlRepository;

    @Mock
    private ControlEffectivenessAssessmentRepository controlEffectivenessAssessmentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ObservationProjectionService service;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private OperationalAsset makeAsset(String uid, AssetType type) {
        var asset = new OperationalAsset(project, uid, "Asset " + uid);
        setField(asset, "id", UUID.randomUUID());
        asset.setAssetType(type);
        return asset;
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

    private Control makeControl(String uid, ControlStatus status) {
        var control = new Control(project, uid, "Control " + uid, ControlFunction.DETECTIVE);
        setField(control, "id", UUID.randomUUID());
        if (status != null) {
            // Use reflection so we don't have to walk a transition chain in tests.
            setField(control, "status", status);
        }
        return control;
    }

    private ControlEffectivenessAssessment makeAssessment(
            Control control,
            LocalDate assessedAt,
            ControlEffectivenessRating design,
            ControlEffectivenessRating operating) {
        var a = new ControlEffectivenessAssessment(
                project, control, "CEA-1", design, operating, assessedAt, "assessor@example.com");
        setField(a, "id", UUID.randomUUID());
        return a;
    }

    @Test
    void assetExposure_happyPath_returnsStructuredResult() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var asset = makeAsset("ASSET-1", AssetType.APPLICATION);
        var obs = makeObservation(asset, asOf.minusSeconds(86400L * 5), null);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of(asset));
        when(observationRepository.findLatestByProjectIdAsOf(projectId, asOf)).thenReturn(List.of(obs));

        ObservationProjectionResult result =
                service.project(projectId, asOf, ObservationProjectionMode.ASSET_EXPOSURE, null, null);

        assertThat(result.analysisKind()).isEqualTo("observation_exposure");
        assertThat(result.project()).isEqualTo("ground-control");
        assertThat(result.asOf()).isEqualTo(asOf);
        assertThat(result.derivationMethod()).isEqualTo("observation-current-state-projection-v1");
        assertThat(result.assetExposures()).hasSize(1);
        assertThat(result.assetExposures().get(0).state()).isEqualTo("CURRENT");
        assertThat(result.assetExposures().get(0).currentObservations()).hasSize(1);
        assertThat(result.assetExposures().get(0).currentObservations().get(0).state())
                .isEqualTo("CURRENT");
        assertThat(result.controlStates()).isEmpty();
        assertThat(result.limitations()).isNotNull();
        // Finding #7: project-wide path must NOT call the per-asset helper.
        verify(observationRepository, never()).findLatestByAssetIdAsOf(any(), any());
        verify(observationRepository, never()).findLatestByAssetId(any(), any());
    }

    @Test
    void assetExposure_noObservations_returnsNoObservationsState() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var asset = makeAsset("ASSET-1", AssetType.APPLICATION);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of(asset));
        when(observationRepository.findLatestByProjectIdAsOf(projectId, asOf)).thenReturn(List.of());

        ObservationProjectionResult result =
                service.project(projectId, asOf, ObservationProjectionMode.ASSET_EXPOSURE, null, null);

        assertThat(result.assetExposures().get(0).state()).isEqualTo("NO_OBSERVATIONS");
    }

    @Test
    void assetExposure_singleAsset_returnsOnlyThatAsset() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var asset = makeAsset("ASSET-1", AssetType.APPLICATION);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByIdAndProjectId(asset.getId(), projectId))
                .thenReturn(Optional.of(asset));
        when(observationRepository.findLatestByAssetIdAsOf(asset.getId(), asOf)).thenReturn(List.of());

        ObservationProjectionResult result =
                service.project(projectId, asOf, ObservationProjectionMode.ASSET_EXPOSURE, asset.getId(), null);

        assertThat(result.assetExposures()).hasSize(1);
        assertThat(result.assetExposures().get(0).assetUid()).isEqualTo("ASSET-1");
    }

    @Test
    void controlState_usesAssessmentRatings_notControlStatus() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var control = makeControl("CTRL-1", ControlStatus.OPERATIONAL);
        var assessment = makeAssessment(
                control,
                LocalDate.of(2026, 4, 1),
                ControlEffectivenessRating.EFFECTIVE,
                ControlEffectivenessRating.PARTIALLY_EFFECTIVE);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(controlRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(control));
        when(controlEffectivenessAssessmentRepository
                        .findByProjectIdAndAssessedAtLessThanEqualOrderByControlIdAscAssessedAtDesc(
                                org.mockito.ArgumentMatchers.eq(projectId), any(LocalDate.class)))
                .thenReturn(List.of(assessment));

        ObservationProjectionResult result =
                service.project(projectId, asOf, ObservationProjectionMode.CONTROL_STATE, null, null);

        assertThat(result.analysisKind()).isEqualTo("control_state");
        assertThat(result.controlStates()).hasSize(1);
        assertThat(result.controlStates().get(0).controlStatus()).isEqualTo("OPERATIONAL");
        assertThat(result.controlStates().get(0).designEffectiveness()).isEqualTo("EFFECTIVE");
        assertThat(result.controlStates().get(0).operatingEffectiveness()).isEqualTo("PARTIALLY_EFFECTIVE");
        assertThat(result.controlStates().get(0).state()).isEqualTo("CURRENT");
        // The critical preflight anti-pattern check: a limitations entry must
        // explicitly call out that OPERATIONAL is not treated as evidence.
        assertThat(result.limitations())
                .anyMatch(s -> s.contains("ControlStatus.OPERATIONAL is NOT treated as evidence"));
        // Finding #7: project-wide path must NOT call the per-control helper.
        verify(controlEffectivenessAssessmentRepository, never())
                .findByProjectIdAndControlIdOrderByAssessedAtDesc(any(), any());
    }

    @Test
    void controlState_noAssessment_returnsNoObservationsAndNullRatings() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var control = makeControl("CTRL-1", ControlStatus.OPERATIONAL);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(controlRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(control));
        when(controlEffectivenessAssessmentRepository
                        .findByProjectIdAndAssessedAtLessThanEqualOrderByControlIdAscAssessedAtDesc(
                                org.mockito.ArgumentMatchers.eq(projectId), any(LocalDate.class)))
                .thenReturn(List.of());

        ObservationProjectionResult result =
                service.project(projectId, asOf, ObservationProjectionMode.CONTROL_STATE, null, null);

        assertThat(result.controlStates().get(0).state()).isEqualTo("NO_OBSERVATIONS");
        assertThat(result.controlStates().get(0).designEffectiveness()).isNull();
        assertThat(result.controlStates().get(0).operatingEffectiveness()).isNull();
    }

    @Test
    void controlState_asOfBeforeAssessment_doesNotIncludeIt() {
        Instant asOf = Instant.parse("2026-01-01T00:00:00Z");
        var control = makeControl("CTRL-1", ControlStatus.OPERATIONAL);
        // Assessment is dated AFTER asOf. The controlId-specific path calls
        // findByProjectIdAndControlIdOrderByAssessedAtDesc (which is NOT
        // asOf-bounded — it returns ALL assessments for the control) and then
        // applies pickLatestForAsOf in-service. The mock returns the future
        // assessment so the in-service filter is the dispositive check; if a
        // refactor breaks pickLatestForAsOf the test must see
        // designEffectiveness=EFFECTIVE and fail.
        var assessmentLater = makeAssessment(
                control,
                LocalDate.of(2026, 4, 1),
                ControlEffectivenessRating.EFFECTIVE,
                ControlEffectivenessRating.EFFECTIVE);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(controlRepository.findByIdAndProjectId(control.getId(), projectId)).thenReturn(Optional.of(control));
        when(controlEffectivenessAssessmentRepository.findByProjectIdAndControlIdOrderByAssessedAtDesc(
                        org.mockito.ArgumentMatchers.eq(projectId), org.mockito.ArgumentMatchers.eq(control.getId())))
                .thenReturn(List.of(assessmentLater));

        ObservationProjectionResult result =
                service.project(projectId, asOf, ObservationProjectionMode.CONTROL_STATE, null, control.getId());

        // Assessment is dated AFTER asOf — in-service pickLatestForAsOf must reject it.
        assertThat(result.controlStates().get(0).designEffectiveness()).isNull();
        assertThat(result.controlStates().get(0).operatingEffectiveness()).isNull();
        assertThat(result.controlStates().get(0).state()).isEqualTo("NO_OBSERVATIONS");
    }

    /**
     * Finding #2 (asOf): a project-wide observation projection must use the
     * as-of-bounded bulk method, not the unbounded one.
     */
    @Test
    void assetExposure_projectWide_usesAsOfBoundedBulkMethod() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var asset = makeAsset("ASSET-1", AssetType.APPLICATION);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndArchivedAtIsNull(projectId))
                .thenReturn(List.of(asset));
        when(observationRepository.findLatestByProjectIdAsOf(projectId, asOf)).thenReturn(List.of());

        ObservationProjectionResult result =
                service.project(projectId, asOf, ObservationProjectionMode.ASSET_EXPOSURE, null, null);

        verify(observationRepository).findLatestByProjectIdAsOf(projectId, asOf);
        // Lock in the result-level contract so a broken implementation that
        // called the bulk method but returned a stub/null doesn't silently
        // pass this regression guard.
        assertThat(result).isNotNull();
        assertThat(result.analysisKind()).isEqualTo("observation_exposure");
        assertThat(result.derivationMethod()).isEqualTo("observation-current-state-projection-v1");
        assertThat(result.assetExposures()).isNotNull();
        assertThat(result.assetExposures()).hasSize(1);
    }

    @Test
    void projectNotFound_throws() {
        when(projectRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        service.project(projectId, Instant.now(), ObservationProjectionMode.ASSET_EXPOSURE, null, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void assetNotFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        UUID missing = UUID.randomUUID();
        when(operationalAssetRepository.findByIdAndProjectId(missing, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.project(
                        projectId, Instant.now(), ObservationProjectionMode.ASSET_EXPOSURE, missing, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void controlNotFound_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        UUID missing = UUID.randomUUID();
        when(controlRepository.findByIdAndProjectId(missing, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.project(
                        projectId, Instant.now(), ObservationProjectionMode.CONTROL_STATE, null, missing))
                .isInstanceOf(NotFoundException.class);
    }
}
