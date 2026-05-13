package com.keplerops.groundcontrol.domain.findings.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.findings.model.FindingLink;
import com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository;
import com.keplerops.groundcontrol.domain.findings.repository.FindingRepository;
import com.keplerops.groundcontrol.domain.graph.service.GraphTargetResolverService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FindingLinkService {

    private static final Logger log = LoggerFactory.getLogger(FindingLinkService.class);

    private final FindingLinkRepository linkRepository;
    private final FindingRepository findingRepository;
    private final GraphTargetResolverService graphTargetResolverService;

    public FindingLinkService(
            FindingLinkRepository linkRepository,
            FindingRepository findingRepository,
            GraphTargetResolverService graphTargetResolverService) {
        this.linkRepository = linkRepository;
        this.findingRepository = findingRepository;
        this.graphTargetResolverService = graphTargetResolverService;
    }

    public FindingLink create(UUID projectId, UUID findingId, CreateFindingLinkCommand command) {
        var finding = findingRepository
                .findByIdAndProjectId(findingId, projectId)
                .orElseThrow(() -> new NotFoundException("Finding not found: " + findingId));

        var target = graphTargetResolverService.validateFindingTarget(
                projectId, command.targetType(), command.targetEntityId(), command.targetIdentifier());

        boolean exists = target.internal()
                ? linkRepository.existsByFindingIdAndTargetTypeAndTargetEntityIdAndLinkType(
                        findingId, command.targetType(), target.targetEntityId(), command.linkType())
                : linkRepository.existsByFindingIdAndTargetTypeAndTargetIdentifierAndLinkType(
                        findingId, command.targetType(), target.targetIdentifier(), command.linkType());
        if (exists) {
            throw new ConflictException("Link already exists: "
                    + command.targetType() + ":"
                    + (target.internal() ? target.targetEntityId() : target.targetIdentifier())
                    + " (" + command.linkType() + ") on finding " + finding.getUid());
        }

        var link = new FindingLink(
                finding, command.targetType(), target.targetEntityId(), target.targetIdentifier(), command.linkType());
        if (command.targetUrl() != null) {
            link.setTargetUrl(command.targetUrl());
        }
        if (command.targetTitle() != null) {
            link.setTargetTitle(command.targetTitle());
        }

        var saved = linkRepository.save(link);
        log.info(
                "finding_link_created: finding={} target_type={} target={} link_type={} id={}",
                finding.getUid(),
                command.targetType(),
                target.internal() ? target.targetEntityId() : target.targetIdentifier(),
                command.linkType(),
                saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<FindingLink> listByFinding(UUID projectId, UUID findingId) {
        verifyFindingExists(projectId, findingId);
        return linkRepository.findByFindingId(findingId);
    }

    public void delete(UUID projectId, UUID findingId, UUID linkId) {
        var link = linkRepository
                .findByIdAndFindingProjectId(linkId, projectId)
                .orElseThrow(() -> new NotFoundException("Finding link not found: " + linkId));
        if (!link.getFinding().getId().equals(findingId)) {
            throw new NotFoundException("Link " + linkId + " does not belong to finding " + findingId);
        }
        linkRepository.delete(link);
        log.info("finding_link_deleted: id={} finding={}", linkId, findingId);
    }

    private void verifyFindingExists(UUID projectId, UUID findingId) {
        if (!findingRepository.existsByIdAndProjectId(findingId, projectId)) {
            throw new NotFoundException("Finding not found: " + findingId);
        }
    }
}
