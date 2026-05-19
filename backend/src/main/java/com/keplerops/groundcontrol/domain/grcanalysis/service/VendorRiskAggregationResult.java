package com.keplerops.groundcontrol.domain.grcanalysis.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vendor-risk roll-up over {@link com.keplerops.groundcontrol.domain.assets.model.OperationalAsset}
 * rows whose {@code assetType} is
 * {@link com.keplerops.groundcontrol.domain.assets.state.AssetType#THIRD_PARTY}.
 *
 * <p>Per the preflight there is no first-class vendor aggregate; the response
 * must label {@code assetType: THIRD_PARTY} and surface a {@code limitations}
 * entry making the carve-out explicit (GC-L009).
 */
public record VendorRiskAggregationResult(
        String analysisKind,
        String project,
        Instant asOf,
        String derivationMethod,
        Inputs inputs,
        String assetType,
        List<VendorItem> vendors,
        List<String> limitations) {

    public record Inputs(String project, Instant asOf, int freshnessWindowDays, UUID vendorAssetId) {}

    public record VendorItem(
            UUID assetId,
            String assetUid,
            String name,
            String subtype,
            int openFindingCount,
            String evidenceFreshnessState,
            int currentObservationCount,
            List<UUID> mappedControlIds) {}
}
