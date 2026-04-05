package com.keplerops.groundcontrol.domain.controls.service;

import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.repository.ControlRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlStatus;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ControlService {

    private static final Logger log = LoggerFactory.getLogger(ControlService.class);

    private final ControlRepository controlRepository;
    private final ProjectService projectService;

    public ControlService(ControlRepository controlRepository, ProjectService projectService) {
        this.controlRepository = controlRepository;
        this.projectService = projectService;
    }

    public Control create(CreateControlCommand command) {
        var project = projectService.getById(command.projectId());
        if (controlRepository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException("Control with UID " + command.uid() + " already exists in this project");
        }
        var control = new Control(project, command.uid(), command.title(), command.controlFunction());
        control.setDescription(command.description());
        control.setObjective(command.objective());
        control.setOwner(command.owner());
        control.setImplementationScope(command.implementationScope());
        control.setMethodologyFactors(command.methodologyFactors());
        control.setEffectiveness(command.effectiveness());
        control.setCategory(command.category());
        control.setSource(command.source());
        control = controlRepository.save(control);
        log.info("control_created: uid={} project={}", control.getUid(), project.getIdentifier());
        return control;
    }

    public Control update(UUID projectId, UUID id, UpdateControlCommand command) {
        var control = findOrThrow(projectId, id);
        if (command.title() != null) {
            control.setTitle(command.title());
        }
        if (command.controlFunction() != null) {
            control.setControlFunction(command.controlFunction());
        }
        if (command.description() != null) {
            control.setDescription(command.description());
        }
        if (command.objective() != null) {
            control.setObjective(command.objective());
        }
        if (command.owner() != null) {
            control.setOwner(command.owner());
        }
        if (command.implementationScope() != null) {
            control.setImplementationScope(command.implementationScope());
        }
        if (command.methodologyFactors() != null) {
            control.setMethodologyFactors(command.methodologyFactors());
        }
        if (command.effectiveness() != null) {
            control.setEffectiveness(command.effectiveness());
        }
        if (command.category() != null) {
            control.setCategory(command.category());
        }
        if (command.source() != null) {
            control.setSource(command.source());
        }
        control = controlRepository.save(control);
        log.info("control_updated: uid={} id={}", control.getUid(), control.getId());
        return control;
    }

    @Transactional(readOnly = true)
    public Control getById(UUID projectId, UUID id) {
        return findOrThrow(projectId, id);
    }

    private Control findOrThrow(UUID projectId, UUID id) {
        return controlRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Control not found: " + id));
    }

    @Transactional(readOnly = true)
    public Control getByUid(String uid, UUID projectId) {
        return controlRepository
                .findByProjectIdAndUid(projectId, uid)
                .orElseThrow(() -> new NotFoundException("Control not found: " + uid));
    }

    @Transactional(readOnly = true)
    public List<Control> listByProject(UUID projectId) {
        return controlRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public Control transitionStatus(UUID projectId, UUID id, ControlStatus newStatus) {
        var control = findOrThrow(projectId, id);
        control.transitionStatus(newStatus);
        control = controlRepository.save(control);
        log.info("control_status_changed: uid={} status={}", control.getUid(), newStatus);
        return control;
    }

    public void delete(UUID projectId, UUID id) {
        var control = findOrThrow(projectId, id);
        controlRepository.delete(control);
        log.info("control_deleted: uid={} id={}", control.getUid(), id);
    }
}
