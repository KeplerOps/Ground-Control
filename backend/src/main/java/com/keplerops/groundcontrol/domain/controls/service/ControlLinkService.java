package com.keplerops.groundcontrol.domain.controls.service;

import com.keplerops.groundcontrol.domain.controls.model.ControlLink;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
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
public class ControlLinkService {

    private static final Logger log = LoggerFactory.getLogger(ControlLinkService.class);

    private final ControlLinkRepository controlLinkRepository;
    private final ControlService controlService;
    private final GraphTargetResolverService graphTargetResolverService;

    public ControlLinkService(
            ControlLinkRepository controlLinkRepository,
            ControlService controlService,
            GraphTargetResolverService graphTargetResolverService) {
        this.controlLinkRepository = controlLinkRepository;
        this.controlService = controlService;
        this.graphTargetResolverService = graphTargetResolverService;
    }

    public ControlLink create(UUID projectId, UUID controlId, CreateControlLinkCommand command) {
        var control = controlService.getById(projectId, controlId);

        // Project-scoped target validation closes the cross-project gap the create path
        // had before PR #875 (a caller could otherwise persist a link to an entity in
        // another tenant's project). The resolver owns the internal-vs-external dispatch.
        var target = graphTargetResolverService.validateControlTarget(
                projectId, command.targetType(), command.targetEntityId(), command.targetIdentifier());

        boolean exists = target.internal()
                ? controlLinkRepository.existsByControlIdAndTargetTypeAndTargetEntityIdAndLinkType(
                        controlId, command.targetType(), target.targetEntityId(), command.linkType())
                : controlLinkRepository.existsByControlIdAndTargetTypeAndTargetIdentifierAndLinkType(
                        controlId, command.targetType(), target.targetIdentifier(), command.linkType());
        if (exists) {
            throw new ConflictException("Duplicate control link");
        }

        var link = new ControlLink(
                control, command.targetType(), target.targetEntityId(), target.targetIdentifier(), command.linkType());
        if (command.targetUrl() != null) {
            link.setTargetUrl(command.targetUrl());
        }
        if (command.targetTitle() != null) {
            link.setTargetTitle(command.targetTitle());
        }
        link = controlLinkRepository.save(link);
        log.info(
                "control_link_created: control={} targetType={} target={} linkType={}",
                control.getUid(),
                command.targetType(),
                target.internal() ? target.targetEntityId() : target.targetIdentifier(),
                command.linkType());
        return link;
    }

    @Transactional(readOnly = true)
    public List<ControlLink> listByControl(UUID projectId, UUID controlId, ControlLinkTargetType targetType) {
        controlService.getById(projectId, controlId);
        if (targetType != null) {
            return controlLinkRepository.findByControlIdAndTargetType(controlId, targetType);
        }
        return controlLinkRepository.findByControlId(controlId);
    }

    public void delete(UUID projectId, UUID controlId, UUID linkId) {
        controlService.getById(projectId, controlId);
        var link = controlLinkRepository
                .findByIdAndControlProjectId(linkId, projectId)
                .orElseThrow(() -> new NotFoundException("Control link not found: " + linkId));
        controlLinkRepository.delete(link);
        log.info("control_link_deleted: id={}", linkId);
    }
}
