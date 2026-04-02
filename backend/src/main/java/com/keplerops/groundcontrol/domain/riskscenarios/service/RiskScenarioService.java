package com.keplerops.groundcontrol.domain.riskscenarios.service;

import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RiskScenarioService {

    private static final Logger log = LoggerFactory.getLogger(RiskScenarioService.class);

    private final RiskScenarioRepository riskScenarioRepository;
    private final ProjectService projectService;
    private final TraceabilityLinkRepository traceabilityLinkRepository;

    public RiskScenarioService(
            RiskScenarioRepository riskScenarioRepository,
            ProjectService projectService,
            TraceabilityLinkRepository traceabilityLinkRepository) {
        this.riskScenarioRepository = riskScenarioRepository;
        this.projectService = projectService;
        this.traceabilityLinkRepository = traceabilityLinkRepository;
    }

    public RiskScenario create(CreateRiskScenarioCommand command) {
        var project = projectService.getById(command.projectId());

        if (riskScenarioRepository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException("Risk scenario with UID '" + command.uid() + "' already exists in project "
                    + project.getIdentifier());
        }

        var scenario = new RiskScenario(
                project,
                command.uid(),
                command.title(),
                command.threatSource(),
                command.threatEvent(),
                command.affectedObject(),
                command.consequence(),
                command.timeHorizon(),
                ActorHolder.get());
        if (command.vulnerability() != null) {
            scenario.setVulnerability(command.vulnerability());
        }
        if (command.observationRefs() != null) {
            scenario.setObservationRefs(command.observationRefs());
        }
        if (command.topologyContext() != null) {
            scenario.setTopologyContext(command.topologyContext());
        }

        var saved = riskScenarioRepository.save(scenario);
        log.info(
                "risk_scenario_created: project={} uid={} title={} id={}",
                project.getIdentifier(),
                saved.getUid(),
                saved.getTitle(),
                saved.getId());
        return saved;
    }

    public RiskScenario update(UUID id, UpdateRiskScenarioCommand command) {
        var scenario = getById(id);

        if (command.title() != null) {
            scenario.setTitle(command.title());
        }
        if (command.threatSource() != null) {
            scenario.setThreatSource(command.threatSource());
        }
        if (command.threatEvent() != null) {
            scenario.setThreatEvent(command.threatEvent());
        }
        if (command.affectedObject() != null) {
            scenario.setAffectedObject(command.affectedObject());
        }
        if (command.vulnerability() != null) {
            scenario.setVulnerability(command.vulnerability());
        }
        if (command.consequence() != null) {
            scenario.setConsequence(command.consequence());
        }
        if (command.timeHorizon() != null) {
            scenario.setTimeHorizon(command.timeHorizon());
        }
        if (command.observationRefs() != null) {
            scenario.setObservationRefs(command.observationRefs());
        }
        if (command.topologyContext() != null) {
            scenario.setTopologyContext(command.topologyContext());
        }

        var saved = riskScenarioRepository.save(scenario);
        log.info("risk_scenario_updated: id={} uid={}", saved.getId(), saved.getUid());
        return saved;
    }

    @Transactional(readOnly = true)
    public RiskScenario getById(UUID id) {
        return riskScenarioRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Risk scenario not found: " + id));
    }

    @Transactional(readOnly = true)
    public RiskScenario getByUid(String uid, UUID projectId) {
        return riskScenarioRepository
                .findByProjectIdAndUid(projectId, uid)
                .orElseThrow(() -> new NotFoundException("Risk scenario not found: " + uid));
    }

    @Transactional(readOnly = true)
    public List<RiskScenario> listByProject(UUID projectId) {
        return riskScenarioRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public RiskScenario transitionStatus(UUID id, RiskScenarioStatus newStatus) {
        var scenario = getById(id);
        scenario.transitionStatus(newStatus);
        var saved = riskScenarioRepository.save(scenario);
        log.info(
                "risk_scenario_status_changed: id={} uid={} status={}",
                saved.getId(),
                saved.getUid(),
                saved.getStatus());
        return saved;
    }

    public void delete(UUID id) {
        var scenario = getById(id);
        riskScenarioRepository.delete(scenario);
        log.info("risk_scenario_deleted: id={} uid={}", scenario.getId(), scenario.getUid());
    }

    @Transactional(readOnly = true)
    public List<Requirement> findLinkedRequirements(UUID id) {
        var scenario = getById(id);
        var links = traceabilityLinkRepository.findByArtifactTypeAndArtifactIdentifierWithRequirement(
                ArtifactType.RISK_SCENARIO, scenario.getUid());
        return links.stream().map(link -> link.getRequirement()).toList();
    }
}
