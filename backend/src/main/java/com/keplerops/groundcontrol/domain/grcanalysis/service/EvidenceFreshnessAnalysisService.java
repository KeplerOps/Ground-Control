package com.keplerops.groundcontrol.domain.grcanalysis.service;

import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.model.ControlTest;
import com.keplerops.groundcontrol.domain.controls.repository.ControlEffectivenessAssessmentRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlTestRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.evidence.model.EvidenceArtifact;
import com.keplerops.groundcontrol.domain.evidence.model.EvidenceSourceRef;
import com.keplerops.groundcontrol.domain.evidence.repository.EvidenceArtifactRepository;
import com.keplerops.groundcontrol.domain.evidence.state.EvidenceSourceKind;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes evidence-freshness state for {@code EvidenceArtifact},
 * {@code Observation}, and {@code ControlTest} per GC-L007.
 *
 * <p>Pure read-only projection over append-only / supersede-aware domain data.
 * Uses {@code derivedAt}, {@code supersededByArtifactId}, source refs,
 * observation {@code observedAt}/{@code expiresAt}, and control-test
 * {@code testDate} - never Envers revisions (preflight anti-pattern).
 */
@Service
@Transactional(readOnly = true)
public class EvidenceFreshnessAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceFreshnessAnalysisService.class);

    static final String ANALYSIS_KIND = "evidence_freshness";
    static final String DERIVATION_METHOD = "evidence-freshness-projection-v1";

    static final String STATE_FRESH = "FRESH";
    static final String STATE_STALE = "STALE";
    static final String STATE_EXPIRED = "EXPIRED";
    static final String STATE_SUPERSEDED = "SUPERSEDED";
    static final String STATE_NO_OBSERVATIONS = "NO_OBSERVATIONS";

    private final EvidenceArtifactRepository evidenceArtifactRepository;
    private final ObservationRepository observationRepository;
    private final ControlTestRepository controlTestRepository;
    private final ControlEffectivenessAssessmentRepository controlEffectivenessAssessmentRepository;
    private final OperationalAssetRepository operationalAssetRepository;
    private final AssetLinkRepository assetLinkRepository;
    private final ControlLinkRepository controlLinkRepository;
    private final ProjectRepository projectRepository;

    @SuppressWarnings("java:S107") // Aggregator of related repos; splitting would create a parallel facade.
    public EvidenceFreshnessAnalysisService(
            EvidenceArtifactRepository evidenceArtifactRepository,
            ObservationRepository observationRepository,
            ControlTestRepository controlTestRepository,
            ControlEffectivenessAssessmentRepository controlEffectivenessAssessmentRepository,
            OperationalAssetRepository operationalAssetRepository,
            AssetLinkRepository assetLinkRepository,
            ControlLinkRepository controlLinkRepository,
            ProjectRepository projectRepository) {
        this.evidenceArtifactRepository = evidenceArtifactRepository;
        this.observationRepository = observationRepository;
        this.controlTestRepository = controlTestRepository;
        this.controlEffectivenessAssessmentRepository = controlEffectivenessAssessmentRepository;
        this.operationalAssetRepository = operationalAssetRepository;
        this.assetLinkRepository = assetLinkRepository;
        this.controlLinkRepository = controlLinkRepository;
        this.projectRepository = projectRepository;
    }

    public EvidenceFreshnessResult analyze(
            UUID projectId,
            Instant asOf,
            int freshnessWindowDays,
            boolean includeSuperseded,
            UUID assetId,
            UUID controlId) {

        Objects.requireNonNull(projectId, "projectId");
        if (freshnessWindowDays <= 0) {
            throw new DomainValidationException(
                    "freshnessWindowDays must be positive",
                    "validation_error",
                    Map.of("parameter", "freshnessWindowDays", "value", freshnessWindowDays));
        }
        Instant effectiveAsOf = asOf != null ? asOf : Instant.now();
        LocalDate asOfDate = effectiveAsOf.atZone(ZoneOffset.UTC).toLocalDate();

        var project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        // Validate that any asset filter is in-project before any asset-scoped lookup.
        if (assetId != null
                && operationalAssetRepository
                        .findByIdAndProjectId(assetId, projectId)
                        .isEmpty()) {
            throw new NotFoundException("Asset not found in project: " + assetId);
        }

        List<String> limitations = new ArrayList<>();

        var artifactItems = projectEvidenceArtifacts(
                projectId, effectiveAsOf, freshnessWindowDays, includeSuperseded, assetId, controlId);
        var observationItems =
                projectObservations(projectId, effectiveAsOf, freshnessWindowDays, assetId, controlId, limitations);
        var controlTestItems =
                projectControlTests(projectId, asOfDate, freshnessWindowDays, assetId, controlId, limitations);

        int fresh = countByState(artifactItems, observationItems, controlTestItems, STATE_FRESH);
        int stale = countByState(artifactItems, observationItems, controlTestItems, STATE_STALE);
        int expired = countByState(artifactItems, observationItems, controlTestItems, STATE_EXPIRED);
        int superseded = countByState(artifactItems, observationItems, controlTestItems, STATE_SUPERSEDED);
        int currentlyValid = fresh + stale;
        var counts =
                new EvidenceFreshnessResult.EvidenceFreshnessCounts(fresh, stale, expired, superseded, currentlyValid);

        if (assetId != null) {
            limitations.add(
                    "evidence-artifact asset filter walks EvidenceSourceRef OBSERVATION sources; artifacts with no observation source are excluded when assetId is set");
        }
        if (!includeSuperseded) {
            limitations.add(
                    "supersededByArtifactId set artifacts excluded from listing by default; pass includeSuperseded=true to surface them");
        }

        log.info(
                "grcanalysis.evidence_freshness analyzed: project={} asOf={} windowDays={} artifacts={} observations={} controlTests={}",
                project.getIdentifier(),
                effectiveAsOf,
                freshnessWindowDays,
                artifactItems.size(),
                observationItems.size(),
                controlTestItems.size());

        return new EvidenceFreshnessResult(
                ANALYSIS_KIND,
                project.getIdentifier(),
                effectiveAsOf,
                DERIVATION_METHOD,
                new EvidenceFreshnessResult.Inputs(
                        project.getIdentifier(),
                        effectiveAsOf,
                        freshnessWindowDays,
                        includeSuperseded,
                        assetId,
                        controlId),
                artifactItems,
                observationItems,
                controlTestItems,
                counts,
                limitations);
    }

    /**
     * Asset-scoped vendor-roll-up helper used by
     * {@link VendorRiskAggregationService} so the vendor view evaluates the
     * same evidence substrate (artifacts + observations) as the main analysis
     * (GC-L007 finding #6). The asset is assumed to be project-scoped by the
     * caller (the vendor service already validates this before calling).
     */
    public AssetScopedFreshnessSummary assetScopedEvidenceFreshness(
            UUID projectId, Instant asOf, int freshnessWindowDays, UUID assetId) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(assetId, "assetId");
        if (freshnessWindowDays <= 0) {
            throw new DomainValidationException("freshnessWindowDays must be positive");
        }
        Instant effectiveAsOf = asOf != null ? asOf : Instant.now();

        List<String> ignored = new ArrayList<>();
        var artifactItems =
                projectEvidenceArtifacts(projectId, effectiveAsOf, freshnessWindowDays, false, assetId, null);
        var observationItems =
                projectObservations(projectId, effectiveAsOf, freshnessWindowDays, assetId, null, ignored);

        int fresh = 0;
        int stale = 0;
        int expired = 0;
        int superseded = 0;
        for (var a : artifactItems) {
            switch (a.state()) {
                case STATE_FRESH -> fresh++;
                case STATE_STALE -> stale++;
                case STATE_SUPERSEDED -> superseded++;
                default -> {}
            }
        }
        for (var o : observationItems) {
            switch (o.state()) {
                case STATE_FRESH -> fresh++;
                case STATE_STALE -> stale++;
                case STATE_EXPIRED -> expired++;
                default -> {}
            }
        }

        String dominant;
        if (artifactItems.isEmpty() && observationItems.isEmpty()) {
            dominant = STATE_NO_OBSERVATIONS;
        } else if (fresh > 0) {
            dominant = STATE_FRESH;
        } else if (expired > 0) {
            dominant = STATE_EXPIRED;
        } else if (superseded > 0 && stale == 0) {
            dominant = STATE_SUPERSEDED;
        } else {
            dominant = STATE_STALE;
        }
        return new AssetScopedFreshnessSummary(fresh, stale, expired, superseded, dominant);
    }

    /** Lightweight vendor-rollup summary; see {@link #assetScopedEvidenceFreshness}. */
    public record AssetScopedFreshnessSummary(
            int fresh, int stale, int expired, int superseded, String dominantState) {}

    private List<EvidenceFreshnessResult.EvidenceArtifactFreshnessItem> projectEvidenceArtifacts(
            UUID projectId,
            Instant asOf,
            int freshnessWindowDays,
            boolean includeSuperseded,
            UUID assetId,
            UUID controlId) {

        Set<UUID> observationIdsForAsset = null;
        if (assetId != null) {
            observationIdsForAsset = new HashSet<>();
            for (Observation obs : observationRepository.findByAssetIdAndObservedAtLessThanEqual(assetId, asOf)) {
                observationIdsForAsset.add(obs.getId());
            }
        }

        Set<UUID> controlTestIdsForControl = null;
        Set<UUID> ceaIdsForControl = null;
        if (controlId != null) {
            controlTestIdsForControl = new HashSet<>();
            for (ControlTest test :
                    controlTestRepository.findByProjectIdAndControlIdAndTestDateLessThanEqualOrderByTestDateDesc(
                            projectId, controlId, asOf.atZone(ZoneOffset.UTC).toLocalDate())) {
                controlTestIdsForControl.add(test.getId());
            }
            ceaIdsForControl = new HashSet<>();
            for (var cea : controlEffectivenessAssessmentRepository.findByProjectIdAndControlIdOrderByAssessedAtDesc(
                    projectId, controlId)) {
                ceaIdsForControl.add(cea.getId());
            }
        }

        List<EvidenceArtifact> all =
                evidenceArtifactRepository.findByProjectIdAndDerivedAtLessThanEqualOrderByDerivedAtDesc(
                        projectId, asOf);
        List<EvidenceFreshnessResult.EvidenceArtifactFreshnessItem> items = new ArrayList<>();
        for (EvidenceArtifact artifact : all) {
            boolean superseded = artifact.getSupersededByArtifactId() != null;
            if (!includeSuperseded && superseded) {
                continue;
            }
            if (assetId != null && !artifactReferencesObservationInSet(artifact, observationIdsForAsset)) {
                continue;
            }
            if (controlId != null && !artifactReferencesControl(artifact, controlTestIdsForControl, ceaIdsForControl)) {
                continue;
            }
            long ageDays = daysBetween(artifact.getDerivedAt(), asOf);
            String state;
            if (superseded) {
                state = STATE_SUPERSEDED;
            } else if (ageDays > freshnessWindowDays) {
                state = STATE_STALE;
            } else {
                state = STATE_FRESH;
            }
            items.add(new EvidenceFreshnessResult.EvidenceArtifactFreshnessItem(
                    artifact.getId(),
                    artifact.getUid(),
                    artifact.getTitle(),
                    artifact.getDerivedAt(),
                    ageDays,
                    state,
                    artifact.getSupersededByArtifactId()));
        }
        return items;
    }

    private boolean artifactReferencesObservationInSet(EvidenceArtifact artifact, Set<UUID> observationIds) {
        if (observationIds == null || observationIds.isEmpty()) {
            return false;
        }
        List<EvidenceSourceRef> sources = artifact.getSources();
        if (sources == null || sources.isEmpty()) {
            return false;
        }
        for (EvidenceSourceRef ref : sources) {
            if (ref.sourceKind() == EvidenceSourceKind.OBSERVATION
                    && ref.sourceEntityId() != null
                    && observationIds.contains(ref.sourceEntityId())) {
                return true;
            }
        }
        return false;
    }

    private boolean artifactReferencesControl(EvidenceArtifact artifact, Set<UUID> controlTestIds, Set<UUID> ceaIds) {
        List<EvidenceSourceRef> sources = artifact.getSources();
        if (sources == null || sources.isEmpty()) {
            return false;
        }
        for (EvidenceSourceRef ref : sources) {
            if (ref.sourceKind() == EvidenceSourceKind.CONTROL_TEST
                    && ref.sourceEntityId() != null
                    && controlTestIds != null
                    && controlTestIds.contains(ref.sourceEntityId())) {
                return true;
            }
            if (ref.sourceKind() == EvidenceSourceKind.CONTROL_EFFECTIVENESS_ASSESSMENT
                    && ref.sourceEntityId() != null
                    && ceaIds != null
                    && ceaIds.contains(ref.sourceEntityId())) {
                return true;
            }
        }
        return false;
    }

    private List<EvidenceFreshnessResult.ObservationFreshnessItem> projectObservations(
            UUID projectId,
            Instant asOf,
            int freshnessWindowDays,
            UUID assetId,
            UUID controlId,
            List<String> limitations) {

        // Asset-only / asset+control / control-only / unfiltered modes are
        // distinct paths. When only controlId is supplied there is no asset-side
        // join from a Control to an Asset in the schema, so we surface an empty
        // list plus a limitation so callers know the section is intentionally
        // empty rather than incorrectly missing data (Finding #3).
        List<Observation> observations;
        if (assetId != null) {
            observations = observationRepository.findByAssetIdAndObservedAtLessThanEqual(assetId, asOf);
        } else if (controlId != null) {
            limitations.add(
                    "observations narrowed by controlId only: no Control->Asset linkage modeled today; observations list is empty for control-only filters");
            return List.of();
        } else {
            observations = observationRepository.findByProjectIdAndObservedAtLessThanEqual(projectId, asOf);
        }
        List<EvidenceFreshnessResult.ObservationFreshnessItem> items = new ArrayList<>();
        for (Observation obs : observations) {
            Instant expiresAt = obs.getExpiresAt();
            long ageDays = daysBetween(obs.getObservedAt(), asOf);
            String state;
            if (expiresAt != null && !asOf.isBefore(expiresAt)) {
                state = STATE_EXPIRED;
            } else if (ageDays > freshnessWindowDays) {
                state = STATE_STALE;
            } else {
                state = STATE_FRESH;
            }
            items.add(new EvidenceFreshnessResult.ObservationFreshnessItem(
                    obs.getId(),
                    obs.getAsset().getId(),
                    obs.getAsset().getUid(),
                    obs.getCategory() != null ? obs.getCategory().name() : null,
                    obs.getObservationKey(),
                    obs.getObservedAt(),
                    expiresAt,
                    ageDays,
                    state));
        }
        return items;
    }

    private List<EvidenceFreshnessResult.ControlTestFreshnessItem> projectControlTests(
            UUID projectId,
            LocalDate asOfDate,
            int freshnessWindowDays,
            UUID assetId,
            UUID controlId,
            List<String> limitations) {

        // Mirror the observation-side logic: with controlId, narrow on
        // controlId; with assetId only, we'd need an Asset->Control join.
        // AssetLink (outbound, target=CONTROL) plus ControlLink (inbound,
        // target=ASSET) both express that linkage, so we union them.
        List<ControlTest> tests;
        if (controlId != null) {
            tests = controlTestRepository.findByProjectIdAndControlIdAndTestDateLessThanEqualOrderByTestDateDesc(
                    projectId, controlId, asOfDate);
        } else if (assetId != null) {
            Set<UUID> controlIdsForAsset = controlIdsLinkedToAsset(projectId, assetId);
            if (controlIdsForAsset.isEmpty()) {
                limitations.add(
                        "control-tests narrowed by assetId only: no AssetLink (target=CONTROL) or ControlLink (target=ASSET) edges resolve to controls; control-tests list is empty");
                return List.of();
            }
            tests = new ArrayList<>();
            for (ControlTest test : controlTestRepository.findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(
                    projectId, asOfDate)) {
                if (controlIdsForAsset.contains(test.getControl().getId())) {
                    tests.add(test);
                }
            }
        } else {
            tests = controlTestRepository.findByProjectIdAndTestDateLessThanEqualOrderByTestDateDesc(
                    projectId, asOfDate);
        }
        List<EvidenceFreshnessResult.ControlTestFreshnessItem> items = new ArrayList<>();
        for (ControlTest test : tests) {
            LocalDate testDate = test.getTestDate();
            Instant testInstant = testDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant asOfInstant = asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            long ageDays = daysBetween(testInstant, asOfInstant);
            String state = ageDays > freshnessWindowDays ? STATE_STALE : STATE_FRESH;
            items.add(new EvidenceFreshnessResult.ControlTestFreshnessItem(
                    test.getId(),
                    test.getUid(),
                    test.getControl().getId(),
                    test.getControl().getUid(),
                    testDate,
                    ageDays,
                    state));
        }
        return items;
    }

    private Set<UUID> controlIdsLinkedToAsset(UUID projectId, UUID assetId) {
        Set<UUID> ids = new HashSet<>();
        // Outbound: AssetLink.targetType = CONTROL, targetEntityId is a Control id.
        for (var link : assetLinkRepository.findByAssetIdAndTargetType(assetId, AssetLinkTargetType.CONTROL)) {
            if (link.getTargetEntityId() != null) {
                ids.add(link.getTargetEntityId());
            }
        }
        // Inbound: ControlLink.targetType = ASSET, targetEntityId = assetId.
        for (var link : controlLinkRepository.findByProjectId(projectId)) {
            if (link.getTargetType() == ControlLinkTargetType.ASSET && assetId.equals(link.getTargetEntityId())) {
                ids.add(link.getControl().getId());
            }
        }
        return ids;
    }

    private static long daysBetween(Instant from, Instant to) {
        if (from == null || to == null) {
            return 0L;
        }
        return Duration.between(from, to).toDays();
    }

    private static int countByState(
            List<EvidenceFreshnessResult.EvidenceArtifactFreshnessItem> artifacts,
            List<EvidenceFreshnessResult.ObservationFreshnessItem> observations,
            List<EvidenceFreshnessResult.ControlTestFreshnessItem> controlTests,
            String state) {
        int total = 0;
        for (var a : artifacts) {
            if (state.equals(a.state())) {
                total++;
            }
        }
        for (var o : observations) {
            if (state.equals(o.state())) {
                total++;
            }
        }
        for (var c : controlTests) {
            if (state.equals(c.state())) {
                total++;
            }
        }
        return total;
    }
}
