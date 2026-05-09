package com.keplerops.groundcontrol.domain.threatmodels.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.graph.service.GraphTargetResolverService;
import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModelLink;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelLinkRepository;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ThreatModelLinkService {

    private static final Logger log = LoggerFactory.getLogger(ThreatModelLinkService.class);

    private final ThreatModelLinkRepository linkRepository;
    private final ThreatModelRepository threatModelRepository;
    private final GraphTargetResolverService graphTargetResolverService;

    public ThreatModelLinkService(
            ThreatModelLinkRepository linkRepository,
            ThreatModelRepository threatModelRepository,
            GraphTargetResolverService graphTargetResolverService) {
        this.linkRepository = linkRepository;
        this.threatModelRepository = threatModelRepository;
        this.graphTargetResolverService = graphTargetResolverService;
    }

    public ThreatModelLink create(UUID projectId, UUID threatModelId, CreateThreatModelLinkCommand command) {
        var threatModel = threatModelRepository
                .findByIdAndProjectId(threatModelId, projectId)
                .orElseThrow(() -> new NotFoundException("Threat model not found: " + threatModelId));

        var target = graphTargetResolverService.validateThreatModelTarget(
                projectId, command.targetType(), command.targetEntityId(), command.targetIdentifier());

        boolean exists = target.internal()
                ? linkRepository.existsByThreatModelIdAndTargetTypeAndTargetEntityIdAndLinkType(
                        threatModelId, command.targetType(), target.targetEntityId(), command.linkType())
                : linkRepository.existsByThreatModelIdAndTargetTypeAndTargetIdentifierAndLinkType(
                        threatModelId, command.targetType(), target.targetIdentifier(), command.linkType());
        if (exists) {
            throw new ConflictException("Link already exists: "
                    + command.targetType() + ":"
                    + (target.internal() ? target.targetEntityId() : target.targetIdentifier())
                    + " (" + command.linkType() + ") on threat model " + threatModel.getUid());
        }

        var link = new ThreatModelLink(
                threatModel,
                command.targetType(),
                target.targetEntityId(),
                target.targetIdentifier(),
                command.linkType());
        if (command.targetUrl() != null) {
            link.setTargetUrl(command.targetUrl());
        }
        if (command.targetTitle() != null) {
            link.setTargetTitle(command.targetTitle());
        }

        var saved = linkRepository.save(link);
        log.info(
                "threat_model_link_created: threat_model={} target_type={} target={} link_type={} id={}",
                threatModel.getUid(),
                command.targetType(),
                target.internal() ? target.targetEntityId() : target.targetIdentifier(),
                command.linkType(),
                saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ThreatModelLink> listByThreatModel(UUID projectId, UUID threatModelId) {
        verifyThreatModelExists(projectId, threatModelId);
        return linkRepository.findByThreatModelId(threatModelId);
    }

    public void delete(UUID projectId, UUID threatModelId, UUID linkId) {
        var link = linkRepository
                .findByIdAndThreatModelProjectId(linkId, projectId)
                .orElseThrow(() -> new NotFoundException("Threat model link not found: " + linkId));
        if (!link.getThreatModel().getId().equals(threatModelId)) {
            throw new NotFoundException("Link " + linkId + " does not belong to threat model " + threatModelId);
        }
        linkRepository.delete(link);
        log.info("threat_model_link_deleted: id={} threat_model={}", linkId, threatModelId);
    }

    private void verifyThreatModelExists(UUID projectId, UUID threatModelId) {
        if (!threatModelRepository.existsByIdAndProjectId(threatModelId, projectId)) {
            throw new NotFoundException("Threat model not found: " + threatModelId);
        }
    }
}
