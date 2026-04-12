package com.keplerops.groundcontrol.domain.threatmodels.service;

import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModel;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelRepository;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelStatus;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ThreatModelService {

    private static final Logger log = LoggerFactory.getLogger(ThreatModelService.class);

    private final ThreatModelRepository threatModelRepository;
    private final ProjectService projectService;

    public ThreatModelService(ThreatModelRepository threatModelRepository, ProjectService projectService) {
        this.threatModelRepository = threatModelRepository;
        this.projectService = projectService;
    }

    public ThreatModel create(CreateThreatModelCommand command) {
        var project = projectService.getById(command.projectId());

        if (threatModelRepository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException("Threat model with UID '" + command.uid() + "' already exists in project "
                    + project.getIdentifier());
        }

        var threatModel = new ThreatModel(
                project,
                command.uid(),
                command.title(),
                command.threatSource(),
                command.threatEvent(),
                command.effect());
        threatModel.setStride(command.stride());
        threatModel.setNarrative(command.narrative());
        threatModel.setCreatedBy(ActorHolder.get());

        var saved = threatModelRepository.save(threatModel);
        log.info(
                "threat_model_created: project={} uid={} title={} id={}",
                project.getIdentifier(),
                saved.getUid(),
                saved.getTitle(),
                saved.getId());
        return saved;
    }

    public ThreatModel update(UUID projectId, UUID id, UpdateThreatModelCommand command) {
        var threatModel = findByIdOrThrow(projectId, id);

        if (command.title() != null) {
            threatModel.setTitle(command.title());
        }
        if (command.threatSource() != null) {
            threatModel.setThreatSource(command.threatSource());
        }
        if (command.threatEvent() != null) {
            threatModel.setThreatEvent(command.threatEvent());
        }
        if (command.effect() != null) {
            threatModel.setEffect(command.effect());
        }
        if (command.stride() != null) {
            threatModel.setStride(command.stride());
        }
        if (command.narrative() != null) {
            threatModel.setNarrative(command.narrative());
        }

        var saved = threatModelRepository.save(threatModel);
        log.info("threat_model_updated: id={} uid={}", saved.getId(), saved.getUid());
        return saved;
    }

    @Transactional(readOnly = true)
    public ThreatModel getById(UUID projectId, UUID id) {
        return findByIdOrThrow(projectId, id);
    }

    @Transactional(readOnly = true)
    public ThreatModel getByUid(String uid, UUID projectId) {
        return threatModelRepository
                .findByProjectIdAndUid(projectId, uid)
                .orElseThrow(() -> new NotFoundException("Threat model not found: " + uid));
    }

    @Transactional(readOnly = true)
    public List<ThreatModel> listByProject(UUID projectId) {
        return threatModelRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public ThreatModel transitionStatus(UUID projectId, UUID id, ThreatModelStatus newStatus) {
        var threatModel = findByIdOrThrow(projectId, id);
        threatModel.transitionStatus(newStatus);
        var saved = threatModelRepository.save(threatModel);
        log.info(
                "threat_model_status_changed: id={} uid={} status={}",
                saved.getId(),
                saved.getUid(),
                saved.getStatus());
        return saved;
    }

    public void delete(UUID projectId, UUID id) {
        var threatModel = findByIdOrThrow(projectId, id);
        threatModelRepository.delete(threatModel);
        log.info("threat_model_deleted: id={} uid={}", threatModel.getId(), threatModel.getUid());
    }

    private ThreatModel findByIdOrThrow(UUID projectId, UUID id) {
        return threatModelRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Threat model not found: " + id));
    }
}
