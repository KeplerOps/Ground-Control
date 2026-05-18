package com.keplerops.groundcontrol.unit.api.grcanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.keplerops.groundcontrol.api.grcanalysis.VendorRiskAggregationResponse;
import com.keplerops.groundcontrol.domain.grcanalysis.service.VendorRiskAggregationResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pure mapping test for the VendorRiskAggregation API DTO. Populates the
 * vendor list with one fully-populated vendor (including open findings,
 * mapped controls, evidence-freshness summary substructure) and asserts every
 * field in every nested record round-trips.
 */
class VendorRiskAggregationResponseTest {

    @Test
    void from_mapsAllFieldsIncludingVendorSubstructure() {
        UUID vendorId = UUID.randomUUID();
        UUID controlA = UUID.randomUUID();
        UUID controlB = UUID.randomUUID();
        UUID inputsVendorId = UUID.randomUUID();
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");

        VendorRiskAggregationResult.VendorItem vendor = new VendorRiskAggregationResult.VendorItem(
                vendorId, "VENDOR-1", "Acme Corp", "saas-provider", 3, "STALE", 5, List.of(controlA, controlB));
        VendorRiskAggregationResult result = new VendorRiskAggregationResult(
                "vendor_risk_aggregation",
                "ground-control",
                asOf,
                "vendor-third-party-rollup-v1",
                new VendorRiskAggregationResult.Inputs("ground-control", asOf, 90, inputsVendorId),
                "THIRD_PARTY",
                List.of(vendor),
                List.of("GC-L009 carve-out", "filtered-vendor-limitation"));

        VendorRiskAggregationResponse response = VendorRiskAggregationResponse.from(result);

        assertThat(response.analysisKind()).isEqualTo("vendor_risk_aggregation");
        assertThat(response.project()).isEqualTo("ground-control");
        assertThat(response.asOf()).isEqualTo(asOf);
        assertThat(response.derivationMethod()).isEqualTo("vendor-third-party-rollup-v1");
        assertThat(response.assetType()).isEqualTo("THIRD_PARTY");

        VendorRiskAggregationResponse.Inputs mappedInputs = response.inputs();
        assertThat(mappedInputs.project()).isEqualTo("ground-control");
        assertThat(mappedInputs.asOf()).isEqualTo(asOf);
        assertThat(mappedInputs.freshnessWindowDays()).isEqualTo(90);
        assertThat(mappedInputs.vendorAssetId()).isEqualTo(inputsVendorId);

        assertThat(response.vendors()).hasSize(1);
        VendorRiskAggregationResponse.VendorItem mapped = response.vendors().get(0);
        assertThat(mapped.assetId()).isEqualTo(vendorId);
        assertThat(mapped.assetUid()).isEqualTo("VENDOR-1");
        assertThat(mapped.name()).isEqualTo("Acme Corp");
        assertThat(mapped.subtype()).isEqualTo("saas-provider");
        assertThat(mapped.openFindingCount()).isEqualTo(3);
        assertThat(mapped.evidenceFreshnessState()).isEqualTo("STALE");
        assertThat(mapped.currentObservationCount()).isEqualTo(5);
        assertThat(mapped.mappedControlIds()).containsExactly(controlA, controlB);

        assertThat(response.limitations()).containsExactly("GC-L009 carve-out", "filtered-vendor-limitation");
    }

    @Test
    void from_emptyVendorList_andNoVendorAssetId_mapCleanly() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        VendorRiskAggregationResult result = new VendorRiskAggregationResult(
                "vendor_risk_aggregation",
                "ground-control",
                asOf,
                "vendor-third-party-rollup-v1",
                new VendorRiskAggregationResult.Inputs("ground-control", asOf, 90, null),
                "THIRD_PARTY",
                List.of(),
                List.of());

        VendorRiskAggregationResponse response = VendorRiskAggregationResponse.from(result);

        assertThat(response.vendors()).isEmpty();
        assertThat(response.limitations()).isEmpty();
        assertThat(response.inputs().vendorAssetId()).isNull();
    }

    @Test
    void from_copiesMappedControlIds_doesNotShareUnderlyingList() {
        Instant asOf = Instant.parse("2026-05-18T00:00:00Z");
        java.util.ArrayList<UUID> mutableControlIds = new java.util.ArrayList<>();
        mutableControlIds.add(UUID.randomUUID());
        VendorRiskAggregationResult.VendorItem vendor = new VendorRiskAggregationResult.VendorItem(
                UUID.randomUUID(), "VENDOR-1", "Vendor", null, 0, "FRESH", 0, mutableControlIds);
        VendorRiskAggregationResult result = new VendorRiskAggregationResult(
                "vendor_risk_aggregation",
                "ground-control",
                asOf,
                "vendor-third-party-rollup-v1",
                new VendorRiskAggregationResult.Inputs("ground-control", asOf, 90, null),
                "THIRD_PARTY",
                List.of(vendor),
                List.of());

        VendorRiskAggregationResponse response = VendorRiskAggregationResponse.from(result);

        // The DTO must hold an immutable copy so the wire shape can't be
        // mutated through the underlying domain list after mapping.
        assertThat(response.vendors().get(0).mappedControlIds()).hasSize(1);
        mutableControlIds.add(UUID.randomUUID());
        assertThat(response.vendors().get(0).mappedControlIds()).hasSize(1);
    }
}
