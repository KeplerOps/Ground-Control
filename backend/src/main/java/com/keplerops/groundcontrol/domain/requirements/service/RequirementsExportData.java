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
}
