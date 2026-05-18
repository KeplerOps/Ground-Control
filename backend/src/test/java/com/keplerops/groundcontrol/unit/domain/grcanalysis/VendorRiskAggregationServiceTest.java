package com.keplerops.groundcontrol.unit.domain.grcanalysis;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.model.AssetLink;
import com.keplerops.groundcontrol.domain.assets.model.OperationalAsset;
import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkType;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlLink;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkType;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.findings.model.Finding;
import com.keplerops.groundcontrol.domain.findings.model.FindingLink;
import com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository;
import com.keplerops.groundcontrol.domain.findings.repository.FindingRepository;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkType;
import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import com.keplerops.groundcontrol.domain.grcanalysis.service.EvidenceFreshnessAnalysisService;
import com.keplerops.groundcontrol.domain.grcanalysis.service.VendorRiskAggregationResult;
import com.keplerops.groundcontrol.domain.grcanalysis.service.VendorRiskAggregationService;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import java.time.Instant;
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
class VendorRiskAggregationServiceTest {

    @Mock
    private OperationalAssetRepository operationalAssetRepository;

    @Mock
    private AssetLinkRepository assetLinkRepository;

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private FindingLinkRepository findingLinkRepository;

    @Mock
    private ControlLinkRepository controlLinkRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private EvidenceFreshnessAnalysisService evidenceFreshnessAnalysisService;

    @InjectMocks
    private VendorRiskAggregationService service;

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
        asset.setSubtype("saas-provider");
        return asset;
    }

    private Finding makeFinding(String uid, FindingStatus status) {
        var f = new Finding(
                project, uid, "Finding " + uid, FindingType.CONTROL_DEFICIENCY, FindingSeverity.HIGH, "desc");
        setField(f, "id", UUID.randomUUID());
        setField(f, "status", status);
        return f;
    }

    private FindingLink makeFindingLinkToAsset(Finding finding, UUID assetId) {
        var link = new FindingLink(finding, FindingLinkTargetType.ASSET, assetId, null, FindingLinkType.AFFECTS);
        setField(link, "id", UUID.randomUUID());
        return link;
    }

    private ControlLink makeControlLinkToAsset(Control control, UUID assetId) {
        var link = new ControlLink(control, ControlLinkTargetType.ASSET, assetId, null, ControlLinkType.MITIGATES);
        setField(link, "id", UUID.randomUUID());
        return link;
    }

    private AssetLink makeAssetLink(OperationalAsset asset, AssetLinkTargetType type, UUID targetEntityId) {
        var link = new AssetLink(asset, type, targetEntityId, null, AssetLinkType.ASSOCIATED);
        setField(link, "id", UUID.randomUUID());
        return link;
    }

    private EvidenceFreshnessAnalysisService.AssetScopedFreshnessSummary freshnessSummary(
            int fresh, int stale, int expired, int superseded, String state) {
        return new EvidenceFreshnessAnalysisService.AssetScopedFreshnessSummary(
                fresh, stale, expired, superseded, state);
    }

    @Test
    void happyPath_returnsStructuredResultWithThirdPartyLabelAndCarveOutLimitation() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var vendor = makeAsset("VENDOR-1", AssetType.THIRD_PARTY);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(
                        projectId, AssetType.THIRD_PARTY))
                .thenReturn(List.of(vendor));
        when(findingRepository.findIdsByProjectIdAndStatusNot(projectId, FindingStatus.VERIFIED_CLOSED))
                .thenReturn(List.of());
        when(findingLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(controlLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(assetLinkRepository.findByProjectIdAndTargetTypeIn(eq(projectId), any()))
                .thenReturn(List.of());
        when(evidenceFreshnessAnalysisService.assetScopedEvidenceFreshness(
                        eq(projectId), eq(asOf), anyInt(), eq(vendor.getId())))
                .thenReturn(freshnessSummary(0, 0, 0, 0, "NO_OBSERVATIONS"));

        VendorRiskAggregationResult result = service.aggregate(projectId, asOf, 90, null);

        assertThat(result.analysisKind()).isEqualTo("vendor_risk_aggregation");
        assertThat(result.project()).isEqualTo("ground-control");
        assertThat(result.asOf()).isEqualTo(asOf);
        assertThat(result.derivationMethod()).isEqualTo("vendor-third-party-rollup-v1");
        assertThat(result.assetType()).isEqualTo("THIRD_PARTY");
        assertThat(result.vendors()).hasSize(1);
        assertThat(result.limitations())
                .anyMatch(s -> s.contains("not a first-class vendor aggregate") && s.contains("GC-L009 carve-out"));
    }

    @Test
    void openFindings_intersectsFindingLinkAssetTargetWithOpenStatus() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var vendor = makeAsset("VENDOR-1", AssetType.THIRD_PARTY);
        var openFinding = makeFinding("FND-1", FindingStatus.OPEN);
        var closedFinding = makeFinding("FND-2", FindingStatus.VERIFIED_CLOSED);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(
                        projectId, AssetType.THIRD_PARTY))
                .thenReturn(List.of(vendor));
        when(findingRepository.findIdsByProjectIdAndStatusNot(projectId, FindingStatus.VERIFIED_CLOSED))
                .thenReturn(List.of(openFinding.getId()));
        when(findingLinkRepository.findByProjectId(projectId))
                .thenReturn(List.of(
                        makeFindingLinkToAsset(openFinding, vendor.getId()),
                        makeFindingLinkToAsset(closedFinding, vendor.getId())));
        when(controlLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(assetLinkRepository.findByProjectIdAndTargetTypeIn(eq(projectId), any()))
                .thenReturn(List.of());
        when(evidenceFreshnessAnalysisService.assetScopedEvidenceFreshness(
                        eq(projectId), any(), anyInt(), eq(vendor.getId())))
                .thenReturn(freshnessSummary(0, 0, 0, 0, "NO_OBSERVATIONS"));

        VendorRiskAggregationResult result = service.aggregate(projectId, asOf, 90, null);

        assertThat(result.vendors().get(0).openFindingCount()).isEqualTo(1);
    }

    /**
     * Finding #5: outbound AssetLink edges from the vendor to a FINDING must
     * be unioned with inbound FindingLink edges; otherwise vendors that were
     * linked from the asset side undercount.
     */
    @Test
    void openFindings_unionsOutboundAssetLinkFindingEdges() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var vendor = makeAsset("VENDOR-1", AssetType.THIRD_PARTY);
        var openFinding = makeFinding("FND-1", FindingStatus.OPEN);
        var outboundLink = makeAssetLink(vendor, AssetLinkTargetType.FINDING, openFinding.getId());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(
                        projectId, AssetType.THIRD_PARTY))
                .thenReturn(List.of(vendor));
        when(findingRepository.findIdsByProjectIdAndStatusNot(projectId, FindingStatus.VERIFIED_CLOSED))
                .thenReturn(List.of(openFinding.getId()));
        when(findingLinkRepository.findByProjectId(projectId)).thenReturn(List.of()); // no inbound
        when(controlLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(assetLinkRepository.findByProjectIdAndTargetTypeIn(eq(projectId), any()))
                .thenReturn(List.of(outboundLink));
        when(evidenceFreshnessAnalysisService.assetScopedEvidenceFreshness(
                        eq(projectId), any(), anyInt(), eq(vendor.getId())))
                .thenReturn(freshnessSummary(0, 0, 0, 0, "NO_OBSERVATIONS"));

        VendorRiskAggregationResult result = service.aggregate(projectId, asOf, 90, null);

        assertThat(result.vendors().get(0).openFindingCount()).isEqualTo(1);
    }

    @Test
    void currentObservations_useFreshnessSummaryCounts() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var vendor = makeAsset("VENDOR-1", AssetType.THIRD_PARTY);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(
                        projectId, AssetType.THIRD_PARTY))
                .thenReturn(List.of(vendor));
        when(findingRepository.findIdsByProjectIdAndStatusNot(projectId, FindingStatus.VERIFIED_CLOSED))
                .thenReturn(List.of());
        when(findingLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(controlLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(assetLinkRepository.findByProjectIdAndTargetTypeIn(eq(projectId), any()))
                .thenReturn(List.of());
        when(evidenceFreshnessAnalysisService.assetScopedEvidenceFreshness(
                        eq(projectId), eq(asOf), anyInt(), eq(vendor.getId())))
                .thenReturn(freshnessSummary(1, 0, 0, 0, "FRESH"));

        VendorRiskAggregationResult result = service.aggregate(projectId, asOf, 90, null);

        assertThat(result.vendors().get(0).currentObservationCount()).isEqualTo(1);
        assertThat(result.vendors().get(0).evidenceFreshnessState()).isEqualTo("FRESH");
    }

    /**
     * Finding #6: evidence freshness must come from the shared helper, not
     * from a local observation-only computation. If the helper says
     * NO_OBSERVATIONS we must not pretend it's FRESH.
     */
    @Test
    void evidenceFreshness_delegatesToSharedHelper() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var vendor = makeAsset("VENDOR-1", AssetType.THIRD_PARTY);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(
                        projectId, AssetType.THIRD_PARTY))
                .thenReturn(List.of(vendor));
        when(findingRepository.findIdsByProjectIdAndStatusNot(projectId, FindingStatus.VERIFIED_CLOSED))
                .thenReturn(List.of());
        when(findingLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(controlLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(assetLinkRepository.findByProjectIdAndTargetTypeIn(eq(projectId), any()))
                .thenReturn(List.of());
        when(evidenceFreshnessAnalysisService.assetScopedEvidenceFreshness(
                        eq(projectId), eq(asOf), anyInt(), eq(vendor.getId())))
                .thenReturn(freshnessSummary(0, 1, 0, 1, "STALE"));

        VendorRiskAggregationResult result = service.aggregate(projectId, asOf, 90, null);

        assertThat(result.vendors().get(0).evidenceFreshnessState()).isEqualTo("STALE");
        verify(evidenceFreshnessAnalysisService)
                .assetScopedEvidenceFreshness(eq(projectId), eq(asOf), anyInt(), eq(vendor.getId()));
    }

    @Test
    void thirdPartyFilter_onlyThirdPartyAssetsSurfaceWhenNoVendorId() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var vendor = makeAsset("VENDOR-1", AssetType.THIRD_PARTY);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(
                        projectId, AssetType.THIRD_PARTY))
                .thenReturn(List.of(vendor));
        when(findingRepository.findIdsByProjectIdAndStatusNot(projectId, FindingStatus.VERIFIED_CLOSED))
                .thenReturn(List.of());
        when(findingLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(controlLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(assetLinkRepository.findByProjectIdAndTargetTypeIn(eq(projectId), any()))
                .thenReturn(List.of());
        when(evidenceFreshnessAnalysisService.assetScopedEvidenceFreshness(
                        eq(projectId), any(), anyInt(), eq(vendor.getId())))
                .thenReturn(freshnessSummary(0, 0, 0, 0, "NO_OBSERVATIONS"));

        VendorRiskAggregationResult result = service.aggregate(projectId, asOf, 90, null);

        assertThat(result.vendors()).hasSize(1);
        assertThat(result.vendors().get(0).assetUid()).isEqualTo("VENDOR-1");
    }

    @Test
    void singleVendorById_rejectsNonThirdPartyAsset() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var notVendor = makeAsset("APP-1", AssetType.APPLICATION);
        UUID notVendorId = notVendor.getId();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByIdAndProjectId(notVendorId, projectId))
                .thenReturn(Optional.of(notVendor));

        assertThatThrownBy(() -> service.aggregate(projectId, asOf, 90, notVendorId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("THIRD_PARTY");
    }

    /**
     * Finding #1: a cross-project vendorAssetId is rejected with
     * NotFoundException (existing behavior, asserted here to lock it in).
     */
    @Test
    void crossProjectVendorAssetId_isRejectedAsNotFound() {
        UUID foreign = UUID.randomUUID();
        Instant now = Instant.now();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByIdAndProjectId(foreign, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aggregate(projectId, now, 90, foreign))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Asset not found in project");
        verify(evidenceFreshnessAnalysisService, never()).assetScopedEvidenceFreshness(any(), any(), anyInt(), any());
    }

    @Test
    void mappedControls_collectedFromControlLinkAssetEdges() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var vendor = makeAsset("VENDOR-1", AssetType.THIRD_PARTY);
        var control = new Control(project, "CTRL-1", "Control 1", ControlFunction.DETECTIVE);
        setField(control, "id", UUID.randomUUID());
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(
                        projectId, AssetType.THIRD_PARTY))
                .thenReturn(List.of(vendor));
        when(findingRepository.findIdsByProjectIdAndStatusNot(projectId, FindingStatus.VERIFIED_CLOSED))
                .thenReturn(List.of());
        when(findingLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(controlLinkRepository.findByProjectId(projectId))
                .thenReturn(List.of(makeControlLinkToAsset(control, vendor.getId())));
        when(assetLinkRepository.findByProjectIdAndTargetTypeIn(eq(projectId), any()))
                .thenReturn(List.of());
        when(evidenceFreshnessAnalysisService.assetScopedEvidenceFreshness(
                        eq(projectId), any(), anyInt(), eq(vendor.getId())))
                .thenReturn(freshnessSummary(0, 0, 0, 0, "NO_OBSERVATIONS"));

        VendorRiskAggregationResult result = service.aggregate(projectId, asOf, 90, null);

        assertThat(result.vendors().get(0).mappedControlIds()).containsExactly(control.getId());
    }

    /** Finding #5: outbound AssetLink to CONTROL also contributes to mappedControlIds. */
    @Test
    void mappedControls_unionsOutboundAssetLinkControlEdges() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        var vendor = makeAsset("VENDOR-1", AssetType.THIRD_PARTY);
        var outboundControlId = UUID.randomUUID();
        var outboundLink = makeAssetLink(vendor, AssetLinkTargetType.CONTROL, outboundControlId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(operationalAssetRepository.findByProjectIdAndAssetTypeAndArchivedAtIsNull(
                        projectId, AssetType.THIRD_PARTY))
                .thenReturn(List.of(vendor));
        when(findingRepository.findIdsByProjectIdAndStatusNot(projectId, FindingStatus.VERIFIED_CLOSED))
                .thenReturn(List.of());
        when(findingLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(controlLinkRepository.findByProjectId(projectId)).thenReturn(List.of());
        when(assetLinkRepository.findByProjectIdAndTargetTypeIn(eq(projectId), any()))
                .thenReturn(List.of(outboundLink));
        when(evidenceFreshnessAnalysisService.assetScopedEvidenceFreshness(
                        eq(projectId), any(), anyInt(), eq(vendor.getId())))
                .thenReturn(freshnessSummary(0, 0, 0, 0, "NO_OBSERVATIONS"));

        VendorRiskAggregationResult result = service.aggregate(projectId, asOf, 90, null);

        assertThat(result.vendors().get(0).mappedControlIds()).containsExactly(outboundControlId);
    }

    @Test
    void projectNotFound_throws() {
        Instant now = Instant.now();
        when(projectRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aggregate(projectId, now, 90, null)).isInstanceOf(NotFoundException.class);
    }

    /** Finding #8: non-positive freshnessWindowDays must throw at the service boundary. */
    @Test
    void invalidFreshnessWindow_throwsDomainValidationException() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> service.aggregate(projectId, now, 0, null))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.aggregate(projectId, now, -1, null))
                .isInstanceOf(DomainValidationException.class);
    }
}
