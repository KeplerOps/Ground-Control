package com.keplerops.groundcontrol.domain.grcanalysis.service;

import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment;
import com.keplerops.groundcontrol.domain.controls.repository.ControlEffectivenessAssessmentRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlRepository;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projects current state from append-only observation history per GC-L007.
 *
 * <p>Two modes:
 * <ul>
 *   <li>{@link ObservationProjectionMode#ASSET_EXPOSURE}: latest non-expired
 *       observations per asset via
 *       {@link ObservationRepository#findLatestByAssetIdAsOf(UUID, Instant)}.</li>
 *   <li>{@link ObservationProjectionMode#CONTROL_STATE}: latest
 *       {@link ControlEffectivenessAssessment} per control. Per the preflight,
 *       this MUST NOT infer effectiveness from
 *       {@link com.keplerops.groundcontrol.domain.controls.state.ControlStatus#OPERATIONAL}.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ObservationProjectionService {

    private static final Logger log = LoggerFactory.getLogger(ObservationProjectionService.class);

    static final String ANALYSIS_KIND_ASSET = "observation_exposure";
    static final String ANALYSIS_KIND_CONTROL = "control_state";
    static final String DERIVATION_METHOD = "observation-current-state-projection-v1";

    static final String STATE_CURRENT = "CURRENT";
    static final String STATE_NO_OBSERVATIONS = "NO_OBSERVATIONS";

    private final ObservationRepository observationRepository;
    private final OperationalAssetRepository operationalAssetRepository;
    private final ControlRepository controlRepository;
    private final ControlEffectivenessAssessmentRepository controlEffectivenessAssessmentRepository;
    private final ProjectRepository projectRepository;

    public ObservationProjectionService(
            ObservationRepository observationRepository,
            OperationalAssetRepository operationalAssetRepository,
            ControlRepository controlRepository,
            ControlEffectivenessAssessmentRepository controlEffectivenessAssessmentRepository,
            ProjectRepository projectRepository) {
        this.observationRepository = observationRepository;
        this.operationalAssetRepository = operationalAssetRepository;
        this.controlRepository = controlRepository;
        this.controlEffectivenessAssessmentRepository = controlEffectivenessAssessmentRepository;
        this.projectRepository = projectRepository;
    }

    public ObservationProjectionResult project(
            UUID projectId, Instant asOf, ObservationProjectionMode mode, UUID assetId, UUID controlId) {

        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(mode, "mode");
        Instant effectiveAsOf = asOf != null ? asOf : Instant.now();

        var project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        List<ObservationProjectionResult.AssetExposureItem> assetExposures = List.of();
        List<ObservationProjectionResult.ControlStateItem> controlStates = List.of();
        List<String> limitations = new ArrayList<>();
        String analysisKind;

        if (mode == ObservationProjectionMode.ASSET_EXPOSURE) {
            analysisKind = ANALYSIS_KIND_ASSET;
            assetExposures = projectAssetExposure(projectId, effectiveAsOf, assetId);
            if (controlId != null) {
                limitations.add(
                        "controlId parameter is ignored when mode=ASSET_EXPOSURE; supplied value did not affect output");
            }
        } else {
            analysisKind = ANALYSIS_KIND_CONTROL;
            controlStates = projectControlState(projectId, effectiveAsOf, controlId);
            limitations.add(
                    "controlEffectiveness is derived from ControlEffectivenessAssessment; ControlStatus.OPERATIONAL is NOT treated as evidence of effectiveness (preflight anti-pattern)");
            if (assetId != null) {
                limitations.add(
                        "assetId parameter is ignored when mode=CONTROL_STATE; supplied value did not affect output");
            }
        }

        log.info(
                "grcanalysis.observation_projection projected: project={} mode={} asOf={} assets={} controls={}",
                project.getIdentifier(),
                mode,
                effectiveAsOf,
                assetExposures.size(),
                controlStates.size());

        return new ObservationProjectionResult(
                analysisKind,
                project.getIdentifier(),
                effectiveAsOf,
                DERIVATION_METHOD,
                new ObservationProjectionResult.Inputs(
                        project.getIdentifier(), effectiveAsOf, mode, assetId, controlId),
                assetExposures,
                controlStates,
                limitations);
    }

    private List<ObservationProjectionResult.AssetExposureItem> projectAssetExposure(
            UUID projectId, Instant asOf, UUID assetId) {

        if (assetId != null) {
            // Single-asset path: validate scope, then one targeted query.
            var asset = operationalAssetRepository
                    .findByIdAndProjectId(assetId, projectId)
                    .orElseThrow(() -> new NotFoundException("Asset not found in project: " + assetId));
            List<Observation> latest = observationRepository.findLatestByAssetIdAsOf(asset.getId(), asOf);
            return List.of(assetExposureItem(asset, latest));
        }

        // Project-wide path: one bulk fetch + in-memory grouping (no per-asset
        // repository calls inside the loop body; GC-L007 finding #7).
        List<OperationalAsset> assets = operationalAssetRepository.findByProjectIdAndArchivedAtIsNull(projectId);
        List<Observation> projectLatest = observationRepository.findLatestByProjectIdAsOf(projectId, asOf);
        Map<UUID, List<Observation>> byAsset = new HashMap<>();
        for (Observation obs : projectLatest) {
            byAsset.computeIfAbsent(obs.getAsset().getId(), k -> new ArrayList<>())
                    .add(obs);
        }
        List<ObservationProjectionResult.AssetExposureItem> items = new ArrayList<>(assets.size());
        for (OperationalAsset asset : assets) {
            items.add(assetExposureItem(asset, byAsset.getOrDefault(asset.getId(), List.of())));
        }
        return items;
    }

    private ObservationProjectionResult.AssetExposureItem assetExposureItem(
            OperationalAsset asset, List<Observation> latest) {
        List<ObservationProjectionResult.CurrentObservation> currents = new ArrayList<>(latest.size());
        for (Observation obs : latest) {
            currents.add(new ObservationProjectionResult.CurrentObservation(
                    obs.getId(),
                    obs.getCategory() != null ? obs.getCategory().name() : null,
                    obs.getObservationKey(),
                    obs.getObservedAt(),
                    obs.getExpiresAt(),
                    STATE_CURRENT));
        }
        String state = currents.isEmpty() ? STATE_NO_OBSERVATIONS : STATE_CURRENT;
        return new ObservationProjectionResult.AssetExposureItem(
                asset.getId(),
                asset.getUid(),
                asset.getAssetType() != null ? asset.getAssetType().name() : null,
                state,
                currents);
    }

    private List<ObservationProjectionResult.ControlStateItem> projectControlState(
            UUID projectId, Instant asOf, UUID controlId) {

        if (controlId != null) {
            var control = controlRepository
                    .findByIdAndProjectId(controlId, projectId)
                    .orElseThrow(() -> new NotFoundException("Control not found in project: " + controlId));
            var assessments = controlEffectivenessAssessmentRepository.findByProjectIdAndControlIdOrderByAssessedAtDesc(
                    projectId, control.getId());
            ControlEffectivenessAssessment latest = pickLatestForAsOf(assessments, asOf);
            return List.of(controlStateItem(control, latest));
        }

        // Project-wide path: one bulk fetch + in-memory grouping (Finding #7).
        List<Control> controls = controlRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        LocalDate asOfDate = asOf.atZone(ZoneOffset.UTC).toLocalDate();
        // Repository returns rows grouped by controlId ASC, assessedAt DESC; we
        // pick the first row per controlId.
        List<ControlEffectivenessAssessment> all =
                controlEffectivenessAssessmentRepository
                        .findByProjectIdAndAssessedAtLessThanEqualOrderByControlIdAscAssessedAtDesc(
                                projectId, asOfDate);
        Map<UUID, ControlEffectivenessAssessment> latestByControl = new LinkedHashMap<>();
        for (ControlEffectivenessAssessment a : all) {
            latestByControl.putIfAbsent(a.getControl().getId(), a);
        }
        List<ObservationProjectionResult.ControlStateItem> items = new ArrayList<>(controls.size());
        for (Control control : controls) {
            items.add(controlStateItem(control, latestByControl.get(control.getId())));
        }
        return items;
    }

    private ControlEffectivenessAssessment pickLatestForAsOf(
            List<ControlEffectivenessAssessment> assessments, Instant asOf) {
        for (ControlEffectivenessAssessment a : assessments) {
            if (a.getAssessedAt() == null) {
                continue;
            }
            // assessedAt is LocalDate; compare as start-of-day UTC.
            var asInstant = a.getAssessedAt().atStartOfDay(ZoneOffset.UTC).toInstant();
            if (!asInstant.isAfter(asOf)) {
                return a; // assessments ordered by assessedAt DESC
            }
        }
        return null;
    }

    private ObservationProjectionResult.ControlStateItem controlStateItem(
            Control control, ControlEffectivenessAssessment latest) {
        String designEff = latest != null && latest.getDesignEffectiveness() != null
                ? latest.getDesignEffectiveness().name()
                : null;
        String operatingEff = latest != null && latest.getOperatingEffectiveness() != null
                ? latest.getOperatingEffectiveness().name()
                : null;
        String state = latest != null ? STATE_CURRENT : STATE_NO_OBSERVATIONS;
        return new ObservationProjectionResult.ControlStateItem(
                control.getId(),
                control.getUid(),
                control.getStatus() != null ? control.getStatus().name() : null,
                designEff,
                operatingEff,
                latest != null ? latest.getAssessedAt() : null,
                state);
    }

    /**
     * Helper for callers that want to surface a structured validation error when
     * an unrecognized mode reaches the service boundary. Jackson enum binding
     * normally turns bad values into HTTP 400 at the controller; this is a
     * defensive backstop.
     */
    @SuppressWarnings("unused")
    private static void rejectUnknownMode(ObservationProjectionMode mode) {
        if (mode == null) {
            throw new DomainValidationException(
                    "mode is required",
                    "validation_error",
                    Map.of("parameter", "mode", "allowed", "ASSET_EXPOSURE,CONTROL_STATE"));
        }
    }
}
