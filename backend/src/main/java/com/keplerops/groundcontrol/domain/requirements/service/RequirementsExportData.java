package com.keplerops.groundcontrol.domain.requirements.service;

import java.time.Instant;
import java.util.List;

/** Snapshot of requirement data for export, decoupled from JPA entities. */
public record RequirementsExportData(
        String projectIdentifier, Instant timestamp, List<RequirementSnapshot> requirements) {

    public record RequirementSnapshot(
            String uid,
            String title,
            String statement,
            String rationale,
            String requirementType,
            String priority,
            String status,
            Integer wave,
            List<TraceabilityLinkSnapshot> traceabilityLinks,
            Instant createdAt,
            Instant updatedAt) {}

    public record TraceabilityLinkSnapshot(
            String artifactType,
            String artifactIdentifier,
            String linkType,
            String artifactUrl,
            String artifactTitle) {}

    public static RequirementsExportData from(String projectIdentifier, List<RequirementExportRecord> records) {
        var snapshots = records.stream()
                .map(r -> {
                    var req = r.requirement();
                    var linkSnapshots = r.traceabilityLinks().stream()
                            .map(link -> new TraceabilityLinkSnapshot(
                                    link.getArtifactType().name(),
                                    link.getArtifactIdentifier(),
                                    link.getLinkType().name(),
                                    link.getArtifactUrl(),
                                    link.getArtifactTitle()))
                            .toList();
                    return new RequirementSnapshot(
                            req.getUid(),
                            req.getTitle(),
                            req.getStatement(),
                            req.getRationale(),
                            req.getRequirementType().name(),
                            req.getPriority().name(),
                            req.getStatus().name(),
                            req.getWave(),
                            linkSnapshots,
                            req.getCreatedAt(),
                            req.getUpdatedAt());
                })
                .toList();
        return new RequirementsExportData(projectIdentifier, Instant.now(), snapshots);
    }
}
