package com.keplerops.groundcontrol.domain.requirements.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
        List<Map<String, Object>> errors) {

    public ImportResult {
        errors = List.copyOf(errors);
    }
}
