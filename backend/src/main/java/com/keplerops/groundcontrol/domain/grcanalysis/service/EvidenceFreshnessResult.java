package com.keplerops.groundcontrol.domain.grcanalysis.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Structured result of an evidence-freshness analysis per GC-L007. Carries the
 * preflight's "Result Contract" fields ({@code analysisKind}, {@code project},
 * {@code asOf}, {@code derivationMethod}, {@code inputs}, structured sections,
 * {@code limitations}) so MCP/agent callers do not get a prose blob.
 *
 * <p>The {@code state} field on each item is a String discriminator
 * ({@code "FRESH"}, {@code "STALE"}, {@code "EXPIRED"}, {@code "SUPERSEDED"},
 * {@code "CURRENT"}, {@code "NO_OBSERVATIONS"}) rather than a Java enum so the
 * ADR-034 enum-mirror surface does not grow for a freshness-projection state.
 */
public record EvidenceFreshnessResult(
        String analysisKind,
        String project,
        Instant asOf,
        String derivationMethod,
        Inputs inputs,
        List<EvidenceArtifactFreshnessItem> evidenceArtifacts,
        List<ObservationFreshnessItem> observations,
        List<ControlTestFreshnessItem> controlTests,
        EvidenceFreshnessCounts counts,
        List<String> limitations) {

    public record Inputs(
            String project,
            Instant asOf,
            int freshnessWindowDays,
            boolean includeSuperseded,
            UUID assetId,
            UUID controlId) {}

    public record EvidenceArtifactFreshnessItem(
            UUID id,
            String uid,
            String title,
            Instant derivedAt,
            long ageDays,
            String state,
            UUID supersededByArtifactId) {}

    public record ObservationFreshnessItem(
            UUID id,
            UUID assetId,
            String assetUid,
            String category,
            String observationKey,
            Instant observedAt,
            Instant expiresAt,
            long ageDays,
            String state) {}

    public record ControlTestFreshnessItem(
            UUID id, String uid, UUID controlId, String controlUid, LocalDate testDate, long ageDays, String state) {}

    public record EvidenceFreshnessCounts(int fresh, int stale, int expired, int superseded, int currentlyValid) {}
}
