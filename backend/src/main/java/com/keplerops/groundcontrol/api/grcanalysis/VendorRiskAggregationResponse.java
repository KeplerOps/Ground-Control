package com.keplerops.groundcontrol.api.grcanalysis;

import com.keplerops.groundcontrol.domain.grcanalysis.service.VendorRiskAggregationResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API DTO for vendor-risk aggregation. Decouples the public JSON contract from
 * the domain service record so future domain refactors do not silently change
 * the wire shape.
 */
public record VendorRiskAggregationResponse(
        String analysisKind,
        String project,
        Instant asOf,
        String derivationMethod,
        Inputs inputs,
        String assetType,
        List<VendorItem> vendors,
        List<String> limitations) {

    public static VendorRiskAggregationResponse from(VendorRiskAggregationResult result) {
        return new VendorRiskAggregationResponse(
                result.analysisKind(),
                result.project(),
                result.asOf(),
                result.derivationMethod(),
                Inputs.from(result.inputs()),
                result.assetType(),
                result.vendors().stream().map(VendorItem::from).toList(),
                List.copyOf(result.limitations()));
    }

    public record Inputs(String project, Instant asOf, int freshnessWindowDays, UUID vendorAssetId) {

        public static Inputs from(VendorRiskAggregationResult.Inputs inputs) {
            return new Inputs(inputs.project(), inputs.asOf(), inputs.freshnessWindowDays(), inputs.vendorAssetId());
        }
    }

    public record VendorItem(
            UUID assetId,
            String assetUid,
            String name,
            String subtype,
            int openFindingCount,
            String evidenceFreshnessState,
            int currentObservationCount,
            List<UUID> mappedControlIds) {

        public static VendorItem from(VendorRiskAggregationResult.VendorItem item) {
            return new VendorItem(
                    item.assetId(),
                    item.assetUid(),
                    item.name(),
                    item.subtype(),
                    item.openFindingCount(),
                    item.evidenceFreshnessState(),
                    item.currentObservationCount(),
                    List.copyOf(item.mappedControlIds()));
        }
    }
}
