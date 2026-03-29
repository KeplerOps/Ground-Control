package com.keplerops.groundcontrol.domain.documents.service;

import com.keplerops.groundcontrol.domain.documents.service.DocumentExportResult.ResolvedRequirement;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.service.SdocParser;
import com.keplerops.groundcontrol.domain.requirements.service.SdocRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports a StrictDoc (.sdoc) document by resolving each requirement UID against the owning
 * project's scope rather than performing a global UID lookup.
 *
 * <p>UID uniqueness is project-scoped throughout the system. A global lookup via {@code
 * findByUid(uid)} would silently bind the wrong requirement when two projects share a UID. This
 * service always delegates to {@link
 * RequirementRepository#findByProjectIdAndUidIgnoreCase(UUID, String)} to prevent that ambiguity.
 */
@Service
@Transactional(readOnly = true)
public class DocumentExportService {

    private final RequirementRepository requirementRepository;

    public DocumentExportService(RequirementRepository requirementRepository) {
        this.requirementRepository = requirementRepository;
    }

    /**
     * Parse {@code sdocContent} and resolve every requirement UID within {@code projectId}.
     *
     * @param projectId the project that owns the document
     * @param sdocContent raw StrictDoc file content
     * @return resolved requirements and any UIDs not found in the project
     */
    public DocumentExportResult export(UUID projectId, String sdocContent) {
        List<SdocRequirement> parsed = SdocParser.parse(sdocContent);

        List<ResolvedRequirement> resolved = new ArrayList<>();
        List<String> unresolvedUids = new ArrayList<>();

        for (SdocRequirement sdocReq : parsed) {
            requirementRepository
                    .findByProjectIdAndUidIgnoreCase(projectId, sdocReq.uid())
                    .ifPresentOrElse(
                            req -> resolved.add(new ResolvedRequirement(sdocReq, req)),
                            () -> unresolvedUids.add(sdocReq.uid()));
        }

        return new DocumentExportResult(resolved, unresolvedUids);
    }
}
