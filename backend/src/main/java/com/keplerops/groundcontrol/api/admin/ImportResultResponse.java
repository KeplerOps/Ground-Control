package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.ImportError;
import com.keplerops.groundcontrol.domain.requirements.service.ImportResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportResultResponse(
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

    public static ImportResultResponse from(ImportResult result) {
        return new ImportResultResponse(
                result.importId(),
                result.importedAt(),
                result.requirementsParsed(),
                result.requirementsCreated(),
                result.requirementsUpdated(),
                result.relationsCreated(),
                result.relationsSkipped(),
                result.traceabilityLinksCreated(),
                result.traceabilityLinksSkipped(),
                result.documentsCreated(),
                result.sectionsCreated(),
                result.sectionContentsCreated(),
                result.errors());
    }
}
