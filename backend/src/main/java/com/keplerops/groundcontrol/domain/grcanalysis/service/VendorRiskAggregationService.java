package com.keplerops.groundcontrol.domain.grcanalysis.service;

import com.keplerops.groundcontrol.domain.assets.model.AssetLink;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.controls.model.ControlLink;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.findings.model.FindingLink;
import com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository;
import com.keplerops.groundcontrol.domain.findings.repository.FindingRepository;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
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
 * Aggregates vendor-risk signal over
 * {@link OperationalAsset} rows whose {@code assetType} is
 * {@link AssetType#THIRD_PARTY} per GC-L007 / GC-L009.
 *
 * <p>Vendors are not a first-class aggregate in the current schema (see
 * preflight). This service must label {@code assetType: THIRD_PARTY} on the
 * response and emit an explicit limitation entry recording the carve-out.
 */
@Service
@Transactional(readOnly = true)
public class VendorRiskAggregationService {

    private static final Logger log = LoggerFactory.getLogger(VendorRiskAggregationService.class);

    static final String ANALYSIS_KIND = "vendor_risk_aggregation";
    static final String DERIVATION_METHOD = "vendor-third-party-rollup-v1";
    static final String CARVE_OUT_LIMITATION =
            "not a first-class vendor aggregate; modeled as OperationalAsset.THIRD_PARTY per GC-L009 carve-out";

    private final OperationalAssetRepository operationalAssetRepository;
    private final AssetLinkRepository assetLinkRepository;
    private final FindingRepository findingRepository;
    private final FindingLinkRepository findingLinkRepository;
    private final ControlLinkRepository controlLinkRepository;
    private final ProjectRepository projectRepository;
    private final EvidenceFreshnessAnalysisService evidenceFreshnessAnalysisService;

    @SuppressWarnings("java:S107") // Aggregator: each repo carries a distinct concern (asset, link sides, evidence).
    public VendorRiskAggregationService(
            OperationalAssetRepository operationalAssetRepository,
            AssetLinkRepository assetLinkRepository,
            FindingRepository findingRepository,
            FindingLinkRepository findingLinkRepository,
            ControlLinkRepository controlLinkRepository,
            ProjectRepository projectRepository,
            EvidenceFreshnessAnalysisService evidenceFreshnessAnalysisService) {
        this.operationalAssetRepository = operationalAssetRepository;
        this.assetLinkRepository = assetLinkRepository;
        this.findingRepository = findingRepository;
        this.findingLinkRepository = findingLinkRepository;
        this.controlLinkRepository = controlLinkRepository;
        this.projectRepository = projectRepository;
        this.evidenceFreshnessAnalysisService = evidenceFreshnessAnalysisService;
    }

    public VendorRiskAggregationResult aggregate(
            UUID projectId, Instant asOf, int freshnessWindowDays, UUID vendorAssetId) {

        Objects.requireNonNull(projectId, "projectId");
        if (freshnessWindowDays <= 0) {
            throw new DomainValidationException(
                    "freshnessWindowDays must be positive",
                    "validation_error",
                    Map.of("parameter", "freshnessWindowDays", "value", freshnessWindowDays));
        }
        Instant effectiveAsOf = asOf != null ? asOf : Instant.now();

        var project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        List<OperationalAsset> vendors;
        if (vendorAssetId != null) {
            var vendor = operationalAssetRepository
                    .findByIdAndProjectId(vendorAssetId, projectId)
                    .orElseThrow(() -> new NotFoundException("Asset not found in project: " + vendorAssetId));
            if (vendor.getAssetType() != AssetType.THIRD_PARTY) {
                throw new NotFoundException("Asset is not a THIRD_PARTY vendor: " + vendorAssetId);
            }
            vendors = List.of(vendor);
        } else {
            vendors = operationalAssetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(
                    projectId, AssetType.THIRD_PARTY);
        }

        // Pull the open-finding id set once for the project; intersect per-vendor
        // via the inbound FindingLink ASSET edges PLUS outbound AssetLink FINDING edges.
        Set<UUID> openFindingIds = new HashSet<>(
                findingRepository.findIdsByProjectIdAndStatusNot(projectId, FindingStatus.VERIFIED_CLOSED));

        List<FindingLink> projectFindingLinks = findingLinkRepository.findByProjectId(projectId);
        List<ControlLink> projectControlLinks = controlLinkRepository.findByProjectId(projectId);

        // Outbound AssetLink edges from any project asset to FINDING / CONTROL.
        // These are pre-grouped by assetId so per-vendor lookups are O(1).
        List<AssetLink> projectAssetLinks = assetLinkRepository.findByProjectIdAndTargetTypeIn(
                projectId, EnumSet.of(AssetLinkTargetType.FINDING, AssetLinkTargetType.CONTROL));
        Map<UUID, List<AssetLink>> assetLinksByAsset = new HashMap<>();
        for (AssetLink link : projectAssetLinks) {
            UUID id = link.getAsset().getId();
            assetLinksByAsset.computeIfAbsent(id, k -> new ArrayList<>()).add(link);
        }

        List<VendorRiskAggregationResult.VendorItem> vendorItems = new ArrayList<>(vendors.size());
        for (OperationalAsset vendor : vendors) {
            List<AssetLink> outbound = assetLinksByAsset.getOrDefault(vendor.getId(), List.of());
            int openFindings = countOpenFindingsForAsset(projectFindingLinks, outbound, openFindingIds, vendor.getId());
            var freshnessSummary = evidenceFreshnessAnalysisService.assetScopedEvidenceFreshness(
                    projectId, effectiveAsOf, freshnessWindowDays, vendor.getId());
            int currentObs = freshnessSummary.fresh() + freshnessSummary.stale() + freshnessSummary.expired();
            String freshnessState = freshnessSummary.dominantState();
            List<UUID> mappedControls = controlIdsLinkedToAsset(projectControlLinks, outbound, vendor.getId());

            vendorItems.add(new VendorRiskAggregationResult.VendorItem(
                    vendor.getId(),
                    vendor.getUid(),
                    vendor.getName(),
                    vendor.getSubtype(),
                    openFindings,
                    freshnessState,
                    currentObs,
                    mappedControls));
        }

        List<String> limitations = new ArrayList<>();
        limitations.add(CARVE_OUT_LIMITATION);
        if (vendorAssetId != null) {
            limitations.add("filtered to a single THIRD_PARTY asset; project-wide vendor coverage not represented");
        }

        log.info(
                "grcanalysis.vendor_risk aggregated: project={} asOf={} windowDays={} vendors={}",
                project.getIdentifier(),
                effectiveAsOf,
                freshnessWindowDays,
                vendorItems.size());

        return new VendorRiskAggregationResult(
                ANALYSIS_KIND,
                project.getIdentifier(),
                effectiveAsOf,
                DERIVATION_METHOD,
                new VendorRiskAggregationResult.Inputs(
                        project.getIdentifier(), effectiveAsOf, freshnessWindowDays, vendorAssetId),
                AssetType.THIRD_PARTY.name(),
                vendorItems,
                limitations);
    }

    private int countOpenFindingsForAsset(
            List<FindingLink> projectFindingLinks,
            List<AssetLink> outboundAssetLinks,
            Set<UUID> openFindingIds,
            UUID assetId) {
        Set<UUID> seen = new HashSet<>();
        // Inbound: FindingLink edges pointing to this asset.
        for (FindingLink link : projectFindingLinks) {
            if (link.getTargetType() == FindingLinkTargetType.ASSET
                    && assetId.equals(link.getTargetEntityId())
                    && openFindingIds.contains(link.getFinding().getId())) {
                seen.add(link.getFinding().getId());
            }
        }
        // Outbound: AssetLink edges pointing from this asset to a FINDING.
        for (AssetLink link : outboundAssetLinks) {
            if (link.getTargetType() == AssetLinkTargetType.FINDING
                    && link.getTargetEntityId() != null
                    && openFindingIds.contains(link.getTargetEntityId())) {
                seen.add(link.getTargetEntityId());
            }
        }
        return seen.size();
    }

    private List<UUID> controlIdsLinkedToAsset(
            List<ControlLink> projectControlLinks, List<AssetLink> outboundAssetLinks, UUID assetId) {
        Set<UUID> ids = new HashSet<>();
        // Inbound: ControlLink.targetType=ASSET, targetEntityId=assetId.
        for (ControlLink link : projectControlLinks) {
            if (link.getTargetType() == ControlLinkTargetType.ASSET && assetId.equals(link.getTargetEntityId())) {
                ids.add(link.getControl().getId());
            }
        }
        // Outbound: AssetLink.targetType=CONTROL, targetEntityId is a control id.
        for (AssetLink link : outboundAssetLinks) {
            if (link.getTargetType() == AssetLinkTargetType.CONTROL && link.getTargetEntityId() != null) {
                ids.add(link.getTargetEntityId());
            }
        }
        return new ArrayList<>(ids);
    }
}
