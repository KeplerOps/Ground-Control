package com.keplerops.groundcontrol.domain.audits.service;

import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.domain.audits.model.Audit;
import com.keplerops.groundcontrol.domain.audits.model.AuditPhase;
import com.keplerops.groundcontrol.domain.audits.repository.AuditLinkRepository;
import com.keplerops.groundcontrol.domain.audits.repository.AuditRepository;
import com.keplerops.groundcontrol.domain.audits.state.AuditStatus;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioLinkRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
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

@Service("auditsAggregateService")
@Transactional
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String DETAIL_FIELD = "field";

    private final AuditRepository auditRepository;
    private final AuditLinkRepository auditLinkRepository;
    private final ProjectService projectService;
    private final AssetLinkRepository assetLinkRepository;
    private final FindingLinkRepository findingLinkRepository;
    private final RiskScenarioLinkRepository riskScenarioLinkRepository;

    public AuditService(
            AuditRepository auditRepository,
            AuditLinkRepository auditLinkRepository,
            ProjectService projectService,
            AssetLinkRepository assetLinkRepository,
            FindingLinkRepository findingLinkRepository,
            RiskScenarioLinkRepository riskScenarioLinkRepository) {
        this.auditRepository = auditRepository;
        this.auditLinkRepository = auditLinkRepository;
        this.projectService = projectService;
        this.assetLinkRepository = assetLinkRepository;
        this.findingLinkRepository = findingLinkRepository;
        this.riskScenarioLinkRepository = riskScenarioLinkRepository;
    }

    public Audit create(CreateAuditCommand command) {
        var project = projectService.getById(command.projectId());

        if (auditRepository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException(
                    "Audit with UID '" + command.uid() + "' already exists in project " + project.getIdentifier());
        }

        validatePhaseDates(command.phases());

        var audit = new Audit(project, command.uid(), command.title(), command.auditType(), command.scopeDescription());
        if (command.objectives() != null) {
            audit.setObjectives(command.objectives());
        }
        if (command.phases() != null) {
            audit.setPhases(command.phases());
        }
        if (command.teamMembers() != null) {
            audit.setTeamMembers(command.teamMembers());
        }
        audit.setCreatedBy(ActorHolder.get());

        var saved = auditRepository.save(audit);
        log.info(
                "audit_created: project={} uid={} type={} id={}",
                project.getIdentifier(),
                saved.getUid(),
                saved.getAuditType(),
                saved.getId());
        return saved;
    }

    public Audit update(UUID projectId, UUID id, UpdateAuditCommand command) {
        var audit = findByIdOrThrow(projectId, id);

        rejectBlankIfPresent("title", command.title());
        rejectBlankIfPresent("scopeDescription", command.scopeDescription());

        if (command.title() != null) {
            audit.setTitle(command.title());
        }
        if (command.auditType() != null) {
            audit.setAuditType(command.auditType());
        }
        if (command.scopeDescription() != null) {
            audit.setScopeDescription(command.scopeDescription());
        }

        if (command.clearObjectives()) {
            audit.setObjectives(List.of());
        } else if (command.objectives() != null) {
            audit.setObjectives(command.objectives());
        }
        if (command.clearPhases()) {
            audit.setPhases(List.of());
        } else if (command.phases() != null) {
            validatePhaseDates(command.phases());
            audit.setPhases(command.phases());
        }
        if (command.clearTeamMembers()) {
            audit.setTeamMembers(List.of());
        } else if (command.teamMembers() != null) {
            audit.setTeamMembers(command.teamMembers());
        }

        var saved = auditRepository.save(audit);
        log.info("audit_updated: id={} uid={}", saved.getId(), saved.getUid());
        return saved;
    }

    private static void rejectBlankIfPresent(String fieldName, String value) {
        if (value != null && value.isBlank()) {
            throw new DomainValidationException(
                    fieldName + " must not be blank when provided",
                    "validation_error",
                    Map.of(DETAIL_FIELD, fieldName));
        }
    }

    private static void validatePhaseDates(List<AuditPhase> phases) {
        if (phases == null) {
            return;
        }
        for (int i = 0; i < phases.size(); i++) {
            var phase = phases.get(i);
            if (phase == null || phase.kind() == null) {
                throw new DomainValidationException(
                        "Phase at index " + i + " is missing required field 'kind'",
                        "validation_error",
                        Map.of(DETAIL_FIELD, "phases[" + i + "].kind"));
            }
            rejectInvertedRange("plannedStart", phase.plannedStart(), "plannedEnd", phase.plannedEnd(), i);
            rejectInvertedRange("actualStart", phase.actualStart(), "actualEnd", phase.actualEnd(), i);
        }
    }

    private static void rejectInvertedRange(
            String startField, java.time.LocalDate start, String endField, java.time.LocalDate end, int index) {
        if (start == null || end == null) {
            return;
        }
        if (end.isBefore(start)) {
            throw new DomainValidationException(
                    "Phase at index " + index + " has " + endField + " before " + startField,
                    "validation_error",
                    Map.of(
                            DETAIL_FIELD,
                            "phases[" + index + "]." + endField,
                            startField,
                            start.toString(),
                            endField,
                            end.toString()));
        }
    }

    @Transactional(readOnly = true)
    public Audit getById(UUID projectId, UUID id) {
        return findByIdOrThrow(projectId, id);
    }

    @Transactional(readOnly = true)
    public Audit getByUid(String uid, UUID projectId) {
        return auditRepository
                .findByProjectIdAndUid(projectId, uid)
                .orElseThrow(() -> new NotFoundException("Audit not found: " + uid));
    }

    @Transactional(readOnly = true)
    public List<Audit> listByProject(UUID projectId) {
        return auditRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public Audit transitionStatus(UUID projectId, UUID id, AuditStatus newStatus) {
        var audit = findByIdOrThrow(projectId, id);
        audit.transitionStatus(newStatus);
        var saved = auditRepository.save(audit);
        log.info("audit_status_changed: id={} uid={} status={}", saved.getId(), saved.getUid(), saved.getStatus());
        return saved;
    }

    public void delete(UUID projectId, UUID id) {
        var audit = findByIdOrThrow(projectId, id);

        var assetUids = assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                AssetLinkTargetType.AUDIT, id, projectId);
        var findingUids = findingLinkRepository.findFindingUidsByTargetTypeAndTargetEntityIdAndProjectId(
                FindingLinkTargetType.AUDIT, id, projectId);
        var scenarioUids = riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                RiskScenarioLinkTargetType.AUDIT_RECORD, id, projectId);

        if (!assetUids.isEmpty() || !findingUids.isEmpty() || !scenarioUids.isEmpty()) {
            Map<String, Serializable> detail = new LinkedHashMap<>();
            detail.put("auditUid", audit.getUid());
            detail.put("assetCount", assetUids.size());
            detail.put("findingCount", findingUids.size());
            detail.put("scenarioCount", scenarioUids.size());
            detail.put("assetUids", new ArrayList<>(assetUids));
            detail.put("findingUids", new ArrayList<>(findingUids));
            detail.put("scenarioUids", new ArrayList<>(scenarioUids));
            throw new ConflictException(
                    "Audit " + audit.getUid()
                            + " cannot be deleted while reverse links exist. Remove the AssetLink,"
                            + " FindingLink, and RiskScenarioLink references first, then retry.",
                    "audit_referenced",
                    detail);
        }

        var outboundLinks = auditLinkRepository.findByAuditId(id);
        auditLinkRepository.deleteAll(outboundLinks);
        auditRepository.delete(audit);
        log.info(
                "audit_deleted: id={} uid={} outbound_links_deleted={}",
                audit.getId(),
                audit.getUid(),
                outboundLinks.size());
    }

    private Audit findByIdOrThrow(UUID projectId, UUID id) {
        return auditRepository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Audit not found: " + id));
    }
}
