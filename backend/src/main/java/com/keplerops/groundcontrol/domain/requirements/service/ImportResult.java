package com.keplerops.groundcontrol.domain.requirements.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportResult(
        UUID importId,
        Instant importedAt,
        int requirementsParsed,
        int requirementsCreated,
        int requirementsUpdated,
        int relationsCreated,
        int relationsSkipped,
        int traceabilityLinksCreated,
        int traceabilityLinksSkipped,
        int documentsCreated,
        int sectionsCreated,
        int sectionContentsCreated,
        List<ImportError> errors) {

    public ImportResult {
        errors = List.copyOf(errors);
    }
}
