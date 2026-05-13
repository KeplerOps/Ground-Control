package com.keplerops.groundcontrol.domain.controls.service;

import com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment;
import com.keplerops.groundcontrol.domain.controls.repository.ControlEffectivenessAssessmentRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlTestRepository;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for {@link ControlEffectivenessAssessment} per GC-I013.
 *
 * <p>Owns project-scoped CRUD: resolves the parent {@code Control} through {@link ControlService}
 * so cross-project access is rejected before any persistence call, enforces project-scoped UID
 * uniqueness, and emits structured lifecycle log lines. The {@code operatingEffectiveness} field
 * is the stable read target for future GC-T003 risk-scoring code; this service does not perform
 * that scoring.
 */
@Service
@Transactional
public class ControlEffectivenessAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(ControlEffectivenessAssessmentService.class);

    private final ControlEffectivenessAssessmentRepository repository;
    private final ControlTestRepository controlTestRepository;
    private final ControlService controlService;
    private final ProjectService projectService;

    public ControlEffectivenessAssessmentService(
            ControlEffectivenessAssessmentRepository repository,
            ControlTestRepository controlTestRepository,
            ControlService controlService,
            ProjectService projectService) {
        this.repository = repository;
        this.controlTestRepository = controlTestRepository;
        this.controlService = controlService;
        this.projectService = projectService;
    }

    public ControlEffectivenessAssessment create(CreateControlEffectivenessAssessmentCommand command) {
        var project = projectService.getById(command.projectId());
        var control = controlService.getById(project.getId(), command.controlId());
        if (repository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException(
                    "ControlEffectivenessAssessment with UID " + command.uid() + " already exists in this project");
        }
        var assessment = new ControlEffectivenessAssessment(
                project,
                control,
                command.uid(),
                command.designEffectiveness(),
                command.operatingEffectiveness(),
                command.assessedAt(),
                command.assessor());
        assessment.setRationale(command.rationale());
        assessment.setNotes(command.notes());
        assessment.setSupportingTestIds(
                validateSupportingTestIds(project.getId(), control.getId(), command.supportingTestIds()));
        assessment = repository.save(assessment);
        log.info(
                "control_effectiveness_assessment_created: uid={} control={} project={} supportingTests={}",
                assessment.getUid(),
                control.getUid(),
                project.getIdentifier(),
                assessment.getSupportingTestIds() == null
                        ? 0
                        : assessment.getSupportingTestIds().size());
        return assessment;
    }

    public ControlEffectivenessAssessment update(
            UUID projectId, UUID id, UpdateControlEffectivenessAssessmentCommand command) {
        var assessment = findOrThrow(projectId, id);
        if (command.designEffectiveness() != null) {
            assessment.setDesignEffectiveness(command.designEffectiveness());
        }
        if (command.operatingEffectiveness() != null) {
            assessment.setOperatingEffectiveness(command.operatingEffectiveness());
        }
        if (command.assessedAt() != null) {
            assessment.setAssessedAt(command.assessedAt());
        }
        if (command.assessor() != null) {
            assessment.setAssessor(command.assessor());
        }
        if (command.rationale() != null) {
            assessment.setRationale(command.rationale());
        }
        if (command.notes() != null) {
            assessment.setNotes(command.notes());
        }
        if (command.supportingTestIds() != null) {
            assessment.setSupportingTestIds(
                    validateSupportingTestIds(projectId, assessment.getControl().getId(), command.supportingTestIds()));
        }
        assessment = repository.save(assessment);
        log.info("control_effectiveness_assessment_updated: uid={} id={}", assessment.getUid(), assessment.getId());
        return assessment;
    }

    /**
     * Validate that every supporting test UUID resolves to a {@code ControlTest} that belongs to
     * the assessment's parent {@code controlId} in the same project, and return the canonical
     * string-list form to persist. Constraining to the same control keeps the rating semantically
     * tied to one control: an assessment whose {@code OF_CONTROL} edge points at control A cannot
     * cite a test whose own {@code OF_CONTROL} points at control B. Returns an empty list when
     * the input is null or empty. Duplicate IDs are de-duplicated while preserving insertion
     * order; null elements are rejected defensively (DTO-level {@code @NotNull} is the primary
     * guard, this is belt-and-suspenders).
     */
    private List<String> validateSupportingTestIds(UUID projectId, UUID controlId, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        // List.of(...) is an immutable List12 that throws NPE on contains(null) rather than
        // returning false (JDK API quirk). Iterate explicitly so the null check survives any
        // collection implementation a caller hands us.
        for (UUID id : ids) {
            if (id == null) {
                throw new DomainValidationException(
                        "supportingTestIds must not contain null elements",
                        "validation_error",
                        java.util.Map.of("field", "supportingTestIds"));
            }
        }
        var deduped = new LinkedHashSet<>(ids);
        var missing = new ArrayList<UUID>();
        for (var testId : deduped) {
            if (controlTestRepository
                    .findByIdAndProjectIdAndControlId(testId, projectId, controlId)
                    .isEmpty()) {
                missing.add(testId);
            }
        }
        if (!missing.isEmpty()) {
            java.util.Map<String, java.io.Serializable> detail = new java.util.LinkedHashMap<>();
            detail.put("field", "supportingTestIds");
            detail.put(
                    "missingIds",
                    new ArrayList<>(missing.stream().map(UUID::toString).toList()));
            throw new DomainValidationException(
                    "supportingTestIds must reference ControlTest rows belonging to this assessment's control: "
                            + missing,
                    "validation_error",
                    detail);
        }
        var out = new ArrayList<String>(deduped.size());
        for (var testId : deduped) {
            out.add(testId.toString());
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ControlEffectivenessAssessment getById(UUID projectId, UUID id) {
        return findOrThrow(projectId, id);
    }

    @Transactional(readOnly = true)
    public List<ControlEffectivenessAssessment> listByProject(UUID projectId) {
        return repository.findByProjectIdOrderByAssessedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<ControlEffectivenessAssessment> listByProjectAndControl(UUID projectId, UUID controlId) {
        controlService.getById(projectId, controlId);
        return repository.findByProjectIdAndControlIdOrderByAssessedAtDesc(projectId, controlId);
    }

    public void delete(UUID projectId, UUID id) {
        var assessment = findOrThrow(projectId, id);
        repository.delete(assessment);
        log.info("control_effectiveness_assessment_deleted: uid={} id={}", assessment.getUid(), id);
    }

    private ControlEffectivenessAssessment findOrThrow(UUID projectId, UUID id) {
        return repository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("ControlEffectivenessAssessment not found: " + id));
    }
}
