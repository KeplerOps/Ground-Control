package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TraceabilityService {

    private final RequirementRepository requirementRepository;
    private final TraceabilityLinkRepository traceabilityLinkRepository;

    public TraceabilityService(
            RequirementRepository requirementRepository, TraceabilityLinkRepository traceabilityLinkRepository) {
        this.requirementRepository = requirementRepository;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
    }

    public TraceabilityLink createLink(UUID requirementId, CreateTraceabilityLinkCommand command) {
        var requirement = requirementRepository
                .findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Requirement not found: " + requirementId));

        var link = new TraceabilityLink(
                requirement, command.artifactType(), command.artifactIdentifier(), command.linkType());
        if (command.artifactUrl() != null) {
            link.setArtifactUrl(command.artifactUrl());
        }
        if (command.artifactTitle() != null) {
            link.setArtifactTitle(command.artifactTitle());
        }
        return traceabilityLinkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public List<TraceabilityLink> getLinksForRequirement(UUID requirementId) {
        if (!requirementRepository.existsById(requirementId)) {
            throw new NotFoundException("Requirement not found: " + requirementId);
        }
        return traceabilityLinkRepository.findByRequirementId(requirementId);
    }

    public void deleteLink(UUID linkId) {
        var link = traceabilityLinkRepository
                .findById(linkId)
                .orElseThrow(() -> new NotFoundException("Traceability link not found: " + linkId));
        traceabilityLinkRepository.delete(link);
    }
}
