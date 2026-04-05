package com.keplerops.groundcontrol.domain.controls.service;

import com.keplerops.groundcontrol.domain.controls.model.ControlLink;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkType;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
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

    public ControlLinkService(ControlLinkRepository controlLinkRepository, ControlService controlService) {
        this.controlLinkRepository = controlLinkRepository;
        this.controlService = controlService;
    }

    public ControlLink create(
            UUID projectId,
            UUID controlId,
            ControlLinkTargetType targetType,
            UUID targetEntityId,
            String targetIdentifier,
            ControlLinkType linkType,
            String targetUrl,
            String targetTitle) {
        var control = controlService.getById(projectId, controlId);

        if (targetEntityId != null) {
            if (controlLinkRepository.existsByControlIdAndTargetTypeAndTargetEntityIdAndLinkType(
                    controlId, targetType, targetEntityId, linkType)) {
                throw new ConflictException("Duplicate control link");
            }
        } else if (targetIdentifier != null) {
            if (controlLinkRepository.existsByControlIdAndTargetTypeAndTargetIdentifierAndLinkType(
                    controlId, targetType, targetIdentifier, linkType)) {
                throw new ConflictException("Duplicate control link");
            }
        }

        var link = new ControlLink(control, targetType, targetEntityId, targetIdentifier, linkType);
        if (targetUrl != null) {
            link.setTargetUrl(targetUrl);
        }
        if (targetTitle != null) {
            link.setTargetTitle(targetTitle);
        }
        link = controlLinkRepository.save(link);
        log.info("control_link_created: control={} targetType={} linkType={}", control.getUid(), targetType, linkType);
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
