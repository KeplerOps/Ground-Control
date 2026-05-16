package com.keplerops.groundcontrol.domain.findings.service;

import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.findings.model.Finding;
import com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository;
import com.keplerops.groundcontrol.domain.findings.repository.FindingRepository;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioLinkRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelLinkRepository;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FindingService {

    private static final Logger log = LoggerFactory.getLogger(FindingService.class);

    private final FindingRepository findingRepository;
    private final FindingLinkRepository findingLinkRepository;
    private final ProjectService projectService;
    private final AssetLinkRepository assetLinkRepository;
    private final ControlLinkRepository controlLinkRepository;
    private final RiskScenarioLinkRepository riskScenarioLinkRepository;
    private final ThreatModelLinkRepository threatModelLinkRepository;

    public FindingService(
            FindingRepository findingRepository,
            FindingLinkRepository findingLinkRepository,
            ProjectService projectService,
            AssetLinkRepository assetLinkRepository,
            ControlLinkRepository controlLinkRepository,
            RiskScenarioLinkRepository riskScenarioLinkRepository,
            ThreatModelLinkRepository threatModelLinkRepository) {
        this.findingRepository = findingRepository;
        this.findingLinkRepository = findingLinkRepository;
        this.projectService = projectService;
        this.assetLinkRepository = assetLinkRepository;
        this.controlLinkRepository = controlLinkRepository;
        this.riskScenarioLinkRepository = riskScenarioLinkRepository;
        this.threatModelLinkRepository = threatModelLinkRepository;
    }

    public Finding create(CreateFindingCommand command) {
        var project = projectService.getById(command.projectId());

        if (findingRepository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException(
                    "Finding with UID '" + command.uid() + "' already exists in project " + project.getIdentifier());
        }

        var finding = new Finding(
                project,
                command.uid(),
                command.title(),
                command.findingType(),
                command.severity(),
                command.description());
        finding.setRootCauseAnalysis(command.rootCauseAnalysis());
        finding.setOwner(command.owner());
        finding.setDueDate(command.dueDate());
        finding.setCreatedBy(ActorHolder.get());

        var saved = findingRepository.save(finding);
        log.info(
                "finding_created: project={} uid={} type={} severity={} id={}",
                project.getIdentifier(),
                saved.getUid(),
                saved.getFindingType(),
                saved.getSeverity(),
                saved.getId());
        return saved;
    }

    public Finding update(UUID projectId, UUID id, UpdateFindingCommand command) {
        var finding = findByIdOrThrow(projectId, id);

        // Required fields: present-and-non-blank or absent. Reject blank strings so
        // partial updates can't corrupt records the create path would refuse.
        rejectBlankIfPresent("title", command.title());
        rejectBlankIfPresent("description", command.description());

        if (command.title() != null) {
            finding.setTitle(command.title());
        }
        if (command.findingType() != null) {
            finding.setFindingType(command.findingType());
        }
        if (command.severity() != null) {
            finding.setSeverity(command.severity());
        }
        if (command.description() != null) {
            finding.setDescription(command.description());
        }

        if (command.clearRootCauseAnalysis()) {
            finding.setRootCauseAnalysis(null);
        } else if (command.rootCauseAnalysis() != null) {
            finding.setRootCauseAnalysis(command.rootCauseAnalysis());
        }
        if (command.clearOwner()) {
            finding.setOwner(null);
        } else if (command.owner() != null) {
            finding.setOwner(command.owner());
        }
        if (command.clearDueDate()) {
            finding.setDueDate(null);
        } else if (command.dueDate() != null) {
            finding.setDueDate(command.dueDate());
        }

        var saved = findingRepository.save(finding);
        log.info("finding_updated: id={} uid={}", saved.getId(), saved.getUid());
        return saved;
    }

    private static void rejectBlankIfPresent(String fieldName, String value) {
        if (value != null && value.isBlank()) {
            throw new DomainValidationException(
                    fieldName + " must not be blank when provided", "validation_error", Map.of("field", fieldName));
        }
    }

    @Transactional(readOnly = true)
    public Finding getById(UUID projectId, UUID id) {
        return findByIdOrThrow(projectId, id);
    }

    @Transactional(readOnly = true)
    public Finding getByUid(String uid, UUID projectId) {
        return findingRepository
                .findByProjectIdAndUid(projectId, uid)
                .orElseThrow(() -> new NotFoundException("Finding not found: " + uid));
    }

    @Transactional(readOnly = true)
    public List<Finding> listByProject(UUID projectId) {
        return findingRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public Finding transitionStatus(UUID projectId, UUID id, FindingStatus newStatus) {
        var finding = findByIdOrThrow(projectId, id);
        finding.transitionStatus(newStatus);
        var saved = findingRepository.save(finding);
        log.info("finding_status_changed: id={} uid={} status={}", saved.getId(), saved.getUid(), saved.getStatus());
        return saved;
    }

    public void delete(UUID projectId, UUID id) {
        var finding = findByIdOrThrow(projectId, id);

        var assetUids = assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                AssetLinkTargetType.FINDING, id, projectId);
        var controlUids = controlLinkRepository.findControlUidsByTargetTypeAndTargetEntityIdAndProjectId(
                ControlLinkTargetType.FINDING, id, projectId);
        var scenarioUids = riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                RiskScenarioLinkTargetType.FINDING, id, projectId);
        var threatModelUids = threatModelLinkRepository.findThreatModelUidsByTargetTypeAndTargetEntityIdAndProjectId(
                ThreatModelLinkTargetType.FINDING, id, projectId);
        if (!assetUids.isEmpty() || !controlUids.isEmpty() || !scenarioUids.isEmpty() || !threatModelUids.isEmpty()) {
            Map<String, Serializable> detail = new LinkedHashMap<>();
            detail.put("findingUid", finding.getUid());
            detail.put("assetCount", assetUids.size());
            detail.put("controlCount", controlUids.size());
            detail.put("scenarioCount", scenarioUids.size());
            detail.put("threatModelCount", threatModelUids.size());
            detail.put("assetUids", new ArrayList<>(assetUids));
            detail.put("controlUids", new ArrayList<>(controlUids));
            detail.put("scenarioUids", new ArrayList<>(scenarioUids));
            detail.put("threatModelUids", new ArrayList<>(threatModelUids));
            throw new ConflictException(
                    "Finding " + finding.getUid()
                            + " cannot be deleted while reverse links exist. Remove the AssetLink,"
                            + " ControlLink, RiskScenarioLink, and ThreatModelLink references first,"
                            + " then retry.",
                    "finding_referenced",
                    detail);
        }

        // Delete outbound links through the repository before the parent so Envers
        // writes delete revisions for each FindingLink. The migration's FK has no
        // ON DELETE CASCADE; a DB-level cascade would bypass Hibernate and leave
        // finding_link_audit incomplete for the parent-delete path (ADR-038).
        var outboundLinks = findingLinkRepository.findByFindingId(id);
        findingLinkRepository.deleteAll(outboundLinks);
        findingRepository.delete(finding);
        log.info(
                "finding_deleted: id={} uid={} outbound_links_deleted={}",
                finding.getId(),
                finding.getUid(),
                outboundLinks.size());
    }

    private Finding findByIdOrThrow(UUID projectId, UUID id) {
        return findingRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Finding not found: " + id));
    }
}
