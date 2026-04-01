package com.keplerops.groundcontrol.domain.adrs.service;

import com.keplerops.groundcontrol.domain.adrs.model.ArchitectureDecisionRecord;
import com.keplerops.groundcontrol.domain.adrs.repository.AdrRepository;
import com.keplerops.groundcontrol.domain.adrs.state.AdrStatus;
import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdrService {

    private static final Logger log = LoggerFactory.getLogger(AdrService.class);

    private final AdrRepository adrRepository;
    private final ProjectService projectService;
    private final TraceabilityLinkRepository traceabilityLinkRepository;

    public AdrService(
            AdrRepository adrRepository,
            ProjectService projectService,
            TraceabilityLinkRepository traceabilityLinkRepository) {
        this.adrRepository = adrRepository;
        this.projectService = projectService;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
    }

    public ArchitectureDecisionRecord create(CreateAdrCommand command) {
        var project = projectService.getById(command.projectId());

        if (adrRepository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException(
                    "ADR with UID '" + command.uid() + "' already exists in project " + project.getIdentifier());
        }

        var adr = new ArchitectureDecisionRecord(
                project,
                command.uid(),
                command.title(),
                command.decisionDate(),
                command.context(),
                command.decision(),
                command.consequences(),
                ActorHolder.get());
        var saved = adrRepository.save(adr);
        log.info(
                "adr_created: project={} uid={} title={} id={}",
                project.getIdentifier(),
                saved.getUid(),
                saved.getTitle(),
                saved.getId());
        return saved;
    }

    public ArchitectureDecisionRecord update(UUID id, UpdateAdrCommand command) {
        var adr = getById(id);

        if (command.title() != null) {
            adr.setTitle(command.title());
        }
        if (command.decisionDate() != null) {
            adr.setDecisionDate(command.decisionDate());
        }
        if (command.context() != null) {
            adr.setContext(command.context());
        }
        if (command.decision() != null) {
            adr.setDecision(command.decision());
        }
        if (command.consequences() != null) {
            adr.setConsequences(command.consequences());
        }
        if (command.supersededBy() != null) {
            adr.setSupersededBy(command.supersededBy());
        }

        var saved = adrRepository.save(adr);
        log.info("adr_updated: id={} uid={}", saved.getId(), saved.getUid());
        return saved;
    }

    @Transactional(readOnly = true)
    public ArchitectureDecisionRecord getById(UUID id) {
        return adrRepository.findById(id).orElseThrow(() -> new NotFoundException("ADR not found: " + id));
    }

    @Transactional(readOnly = true)
    public ArchitectureDecisionRecord getByUid(String uid, UUID projectId) {
        return adrRepository
                .findByProjectIdAndUid(projectId, uid)
                .orElseThrow(() -> new NotFoundException("ADR not found: " + uid));
    }

    @Transactional(readOnly = true)
    public List<ArchitectureDecisionRecord> listByProject(UUID projectId) {
        return adrRepository.findByProjectIdOrderByDecisionDateDesc(projectId);
    }

    public ArchitectureDecisionRecord transitionStatus(UUID id, AdrStatus newStatus) {
        var adr = getById(id);
        adr.transitionStatus(newStatus);
        var saved = adrRepository.save(adr);
        log.info("adr_status_changed: id={} uid={} status={}", saved.getId(), saved.getUid(), saved.getStatus());
        return saved;
    }

    public void delete(UUID id) {
        var adr = getById(id);
        adrRepository.delete(adr);
        log.info("adr_deleted: id={} uid={}", adr.getId(), adr.getUid());
    }

    @Transactional(readOnly = true)
    public List<Requirement> findLinkedRequirements(UUID id) {
        var adr = getById(id);
        var links = traceabilityLinkRepository.findByArtifactTypeAndArtifactIdentifier(ArtifactType.ADR, adr.getUid());
        return links.stream().map(link -> link.getRequirement()).toList();
    }
}
