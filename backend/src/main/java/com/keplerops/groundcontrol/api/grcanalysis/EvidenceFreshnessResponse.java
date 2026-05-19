package com.keplerops.groundcontrol.api.grcanalysis;

import com.keplerops.groundcontrol.domain.grcanalysis.service.EvidenceFreshnessResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API DTO for evidence-freshness analysis. Decouples the public JSON contract
 * from the domain service record so future domain refactors do not silently
 * change the wire shape (preflight + existing api/admin pattern).
 */
public record EvidenceFreshnessResponse(
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

    public static EvidenceFreshnessResponse from(EvidenceFreshnessResult result) {
        return new EvidenceFreshnessResponse(
                result.analysisKind(),
                result.project(),
                result.asOf(),
                result.derivationMethod(),
                Inputs.from(result.inputs()),
                result.evidenceArtifacts().stream()
                        .map(EvidenceArtifactFreshnessItem::from)
                        .toList(),
                result.observations().stream()
                        .map(ObservationFreshnessItem::from)
                        .toList(),
                result.controlTests().stream()
                        .map(ControlTestFreshnessItem::from)
                        .toList(),
                EvidenceFreshnessCounts.from(result.counts()),
                List.copyOf(result.limitations()));
    }

    public record Inputs(
            String project,
            Instant asOf,
            int freshnessWindowDays,
            boolean includeSuperseded,
            UUID assetId,
            UUID controlId) {

        public static Inputs from(EvidenceFreshnessResult.Inputs inputs) {
            return new Inputs(
                    inputs.project(),
                    inputs.asOf(),
                    inputs.freshnessWindowDays(),
                    inputs.includeSuperseded(),
                    inputs.assetId(),
                    inputs.controlId());
        }
    }

    public record EvidenceArtifactFreshnessItem(
            UUID id,
            String uid,
            String title,
            Instant derivedAt,
            long ageDays,
            String state,
            UUID supersededByArtifactId) {

        public static EvidenceArtifactFreshnessItem from(EvidenceFreshnessResult.EvidenceArtifactFreshnessItem item) {
            return new EvidenceArtifactFreshnessItem(
                    item.id(),
                    item.uid(),
                    item.title(),
                    item.derivedAt(),
                    item.ageDays(),
                    item.state(),
                    item.supersededByArtifactId());
        }
    }

    public record ObservationFreshnessItem(
            UUID id,
            UUID assetId,
            String assetUid,
            String category,
            String observationKey,
            Instant observedAt,
            Instant expiresAt,
            long ageDays,
            String state) {

        public static ObservationFreshnessItem from(EvidenceFreshnessResult.ObservationFreshnessItem item) {
            return new ObservationFreshnessItem(
                    item.id(),
                    item.assetId(),
                    item.assetUid(),
                    item.category(),
                    item.observationKey(),
                    item.observedAt(),
                    item.expiresAt(),
                    item.ageDays(),
                    item.state());
        }
    }

    public record ControlTestFreshnessItem(
            UUID id, String uid, UUID controlId, String controlUid, LocalDate testDate, long ageDays, String state) {

        public static ControlTestFreshnessItem from(EvidenceFreshnessResult.ControlTestFreshnessItem item) {
            return new ControlTestFreshnessItem(
                    item.id(),
                    item.uid(),
                    item.controlId(),
                    item.controlUid(),
                    item.testDate(),
                    item.ageDays(),
                    item.state());
        }
    }

    public record EvidenceFreshnessCounts(int fresh, int stale, int expired, int superseded, int currentlyValid) {

        public static EvidenceFreshnessCounts from(EvidenceFreshnessResult.EvidenceFreshnessCounts counts) {
            return new EvidenceFreshnessCounts(
                    counts.fresh(), counts.stale(), counts.expired(), counts.superseded(), counts.currentlyValid());
        }
    }
}
