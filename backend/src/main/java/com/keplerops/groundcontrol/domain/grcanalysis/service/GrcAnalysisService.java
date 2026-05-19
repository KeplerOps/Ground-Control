package com.keplerops.groundcontrol.domain.grcanalysis.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thin orchestrator that delegates to the three GRC analysis services
 * ({@link EvidenceFreshnessAnalysisService},
 * {@link ObservationProjectionService},
 * {@link VendorRiskAggregationService}). Keeps controllers thin and gives the
 * extension seam from the preflight a single class to point at.
 */
@Service
@Transactional(readOnly = true)
public class GrcAnalysisService {

    private final EvidenceFreshnessAnalysisService evidenceFreshnessAnalysisService;
    private final ObservationProjectionService observationProjectionService;
    private final VendorRiskAggregationService vendorRiskAggregationService;

    public GrcAnalysisService(
            EvidenceFreshnessAnalysisService evidenceFreshnessAnalysisService,
            ObservationProjectionService observationProjectionService,
            VendorRiskAggregationService vendorRiskAggregationService) {
        this.evidenceFreshnessAnalysisService = evidenceFreshnessAnalysisService;
        this.observationProjectionService = observationProjectionService;
        this.vendorRiskAggregationService = vendorRiskAggregationService;
    }

    public EvidenceFreshnessResult evidenceFreshness(
            UUID projectId,
            Instant asOf,
            int freshnessWindowDays,
            boolean includeSuperseded,
            UUID assetId,
            UUID controlId) {
        return evidenceFreshnessAnalysisService.analyze(
                projectId, asOf, freshnessWindowDays, includeSuperseded, assetId, controlId);
    }

    public ObservationProjectionResult observationProjection(
            UUID projectId, Instant asOf, ObservationProjectionMode mode, UUID assetId, UUID controlId) {
        return observationProjectionService.project(projectId, asOf, mode, assetId, controlId);
    }

    public VendorRiskAggregationResult vendorRisk(
            UUID projectId, Instant asOf, int freshnessWindowDays, UUID vendorAssetId) {
        return vendorRiskAggregationService.aggregate(projectId, asOf, freshnessWindowDays, vendorAssetId);
    }
}
