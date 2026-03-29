package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.service.SdocRequirement;
import java.util.List;

/**
 * Result of a document export operation.
 *
 * <p>Contains requirements that were successfully resolved within the owning project's scope, and
 * the UIDs from the source document that could not be found in that project.
 */
public record DocumentExportResult(List<ResolvedRequirement> resolved, List<String> unresolvedUids) {

    public DocumentExportResult {
        resolved = List.copyOf(resolved);
        unresolvedUids = List.copyOf(unresolvedUids);
    }

    /**
     * A pairing of the parsed sdoc requirement with its corresponding database entity, resolved
     * within the owning project.
     */
    public record ResolvedRequirement(SdocRequirement sdocRequirement, Requirement requirement) {}
}
