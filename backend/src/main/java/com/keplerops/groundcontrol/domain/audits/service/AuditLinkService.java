package com.keplerops.groundcontrol.domain.audits.service;

import com.keplerops.groundcontrol.domain.audits.model.AuditLink;
import com.keplerops.groundcontrol.domain.audits.repository.AuditLinkRepository;
import com.keplerops.groundcontrol.domain.audits.repository.AuditRepository;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.graph.service.GraphTargetResolverService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLinkService {

    private static final Logger log = LoggerFactory.getLogger(AuditLinkService.class);

    private final AuditLinkRepository linkRepository;
    private final AuditRepository auditRepository;
    private final GraphTargetResolverService graphTargetResolverService;

    public AuditLinkService(
            AuditLinkRepository linkRepository,
            AuditRepository auditRepository,
            GraphTargetResolverService graphTargetResolverService) {
        this.linkRepository = linkRepository;
        this.auditRepository = auditRepository;
        this.graphTargetResolverService = graphTargetResolverService;
    }

    public AuditLink create(UUID projectId, UUID auditId, CreateAuditLinkCommand command) {
        var audit = auditRepository
                .findByIdAndProjectId(auditId, projectId)
                .orElseThrow(() -> new NotFoundException("Audit not found: " + auditId));

        var target = graphTargetResolverService.validateAuditTarget(
                projectId, command.targetType(), command.targetEntityId(), command.targetIdentifier());

        boolean exists = target.internal()
                ? linkRepository.existsByAuditIdAndTargetTypeAndTargetEntityIdAndLinkType(
                        auditId, command.targetType(), target.targetEntityId(), command.linkType())
                : linkRepository.existsByAuditIdAndTargetTypeAndTargetIdentifierAndLinkType(
                        auditId, command.targetType(), target.targetIdentifier(), command.linkType());
        if (exists) {
            throw new ConflictException("Link already exists: "
                    + command.targetType() + ":"
                    + (target.internal() ? target.targetEntityId() : target.targetIdentifier())
                    + " (" + command.linkType() + ") on audit " + audit.getUid());
        }

        var link = new AuditLink(
                audit, command.targetType(), target.targetEntityId(), target.targetIdentifier(), command.linkType());
        if (command.targetUrl() != null) {
            link.setTargetUrl(command.targetUrl());
        }
        if (command.targetTitle() != null) {
            link.setTargetTitle(command.targetTitle());
        }

        var saved = linkRepository.save(link);
        log.info(
                "audit_link_created: audit={} target_type={} target={} link_type={} id={}",
                audit.getUid(),
                command.targetType(),
                target.internal() ? target.targetEntityId() : target.targetIdentifier(),
                command.linkType(),
                saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AuditLink> listByAudit(UUID projectId, UUID auditId) {
        verifyAuditExists(projectId, auditId);
        return linkRepository.findByAuditId(auditId);
    }

    public void delete(UUID projectId, UUID auditId, UUID linkId) {
        var link = linkRepository
                .findByIdAndAuditProjectId(linkId, projectId)
                .orElseThrow(() -> new NotFoundException("Audit link not found: " + linkId));
        if (!link.getAudit().getId().equals(auditId)) {
            throw new NotFoundException("Link " + linkId + " does not belong to audit " + auditId);
        }
        linkRepository.delete(link);
        log.info("audit_link_deleted: id={} audit={}", linkId, auditId);
    }

    private void verifyAuditExists(UUID projectId, UUID auditId) {
        if (!auditRepository.existsByIdAndProjectId(auditId, projectId)) {
            throw new NotFoundException("Audit not found: " + auditId);
        }
    }
}
