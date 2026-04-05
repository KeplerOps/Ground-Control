package com.keplerops.groundcontrol.domain.riskscenarios.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.model.TreatmentPlan;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskRegisterRecordRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.TreatmentPlanRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.TreatmentPlanStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TreatmentPlanService {

    private final TreatmentPlanRepository repository;
    private final RiskRegisterRecordRepository riskRegisterRecordRepository;
    private final RiskScenarioRepository riskScenarioRepository;
    private final ProjectService projectService;

    public TreatmentPlanService(
            TreatmentPlanRepository repository,
            RiskRegisterRecordRepository riskRegisterRecordRepository,
            RiskScenarioRepository riskScenarioRepository,
            ProjectService projectService) {
        this.repository = repository;
        this.riskRegisterRecordRepository = riskRegisterRecordRepository;
        this.riskScenarioRepository = riskScenarioRepository;
        this.projectService = projectService;
    }

    public TreatmentPlan create(CreateTreatmentPlanCommand command) {
        var project = projectService.getById(command.projectId());
        if (repository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException("Treatment plan with UID " + command.uid() + " already exists");
        }
        var record = riskRegisterRecordRepository
                .findByIdAndProjectIdWithScenarios(command.riskRegisterRecordId(), project.getId())
                .orElseThrow(() ->
                        new NotFoundException("Risk register record not found: " + command.riskRegisterRecordId()));
        var plan = new TreatmentPlan(project, command.uid(), command.title(), record, command.strategy());
        if (command.status() != null && command.status() != TreatmentPlanStatus.PLANNED) {
            plan.transitionStatus(command.status());
        }
        applyUpdates(plan, project.getId(), command);
        return repository.save(plan);
    }

    @Transactional(readOnly = true)
    public List<TreatmentPlan> listByProject(UUID projectId) {
        return repository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<TreatmentPlan> listByRiskRegisterRecord(UUID projectId, UUID riskRegisterRecordId) {
        if (riskRegisterRecordRepository
                .findByIdAndProjectIdWithScenarios(riskRegisterRecordId, projectId)
                .isEmpty()) {
            throw new NotFoundException("Risk register record not found: " + riskRegisterRecordId);
        }
        return repository.findByProjectIdAndRiskRegisterRecordIdOrderByCreatedAtDesc(projectId, riskRegisterRecordId);
    }

    @Transactional(readOnly = true)
    public TreatmentPlan getById(UUID projectId, UUID id) {
        return repository
                .findByIdAndProjectId(id, projectId)
                .orElseThrow(() -> new NotFoundException("Treatment plan not found: " + id));
    }

    public TreatmentPlan update(UUID projectId, UUID id, UpdateTreatmentPlanCommand command) {
        var plan = getById(projectId, id);
        if (command.title() != null) {
            plan.setTitle(command.title());
        }
        applyUpdates(plan, projectId, command);
        return repository.save(plan);
    }

    public TreatmentPlan transitionStatus(UUID projectId, UUID id, TreatmentPlanStatus status) {
        var plan = getById(projectId, id);
        plan.transitionStatus(status);
        return repository.save(plan);
    }

    public void delete(UUID projectId, UUID id) {
        repository.delete(getById(projectId, id));
    }

    private void applyUpdates(TreatmentPlan plan, UUID projectId, CreateTreatmentPlanCommand command) {
        applySharedUpdates(
                plan,
                projectId,
                command.riskScenarioId(),
                command.strategy(),
                command.owner(),
                command.rationale(),
                command.dueDate(),
                command.actionItems(),
                command.reassessmentTriggers());
    }

    private void applyUpdates(TreatmentPlan plan, UUID projectId, UpdateTreatmentPlanCommand command) {
        applySharedUpdates(
                plan,
                projectId,
                command.riskScenarioId(),
                command.strategy(),
                command.owner(),
                command.rationale(),
                command.dueDate(),
                command.actionItems(),
                command.reassessmentTriggers());
    }

    private void applySharedUpdates(
            TreatmentPlan plan,
            UUID projectId,
            UUID riskScenarioId,
            com.keplerops.groundcontrol.domain.riskscenarios.state.TreatmentStrategy strategy,
            String owner,
            String rationale,
            java.time.Instant dueDate,
            List<java.util.Map<String, Object>> actionItems,
            List<String> reassessmentTriggers) {
        if (riskScenarioId != null) {
            var scenario = riskScenarioRepository
                    .findByIdAndProjectId(riskScenarioId, projectId)
                    .orElseThrow(() -> new NotFoundException("Risk scenario not found: " + riskScenarioId));
            if (!plan.getRiskRegisterRecord().getRiskScenarios().isEmpty()
                    && plan.getRiskRegisterRecord().getRiskScenarios().stream()
                            .noneMatch(candidate -> candidate.getId().equals(scenario.getId()))) {
                throw new DomainValidationException(
                        "Treatment plan scenario must belong to the linked risk register record");
            }
            plan.setRiskScenario(scenario);
        }
        if (strategy != null) {
            plan.setStrategy(strategy);
        }
        if (owner != null) {
            plan.setOwner(owner);
        }
        if (rationale != null) {
            plan.setRationale(rationale);
        }
        if (dueDate != null) {
            plan.setDueDate(dueDate);
        }
        if (actionItems != null) {
            plan.setActionItems(actionItems);
        }
        if (reassessmentTriggers != null) {
            plan.setReassessmentTriggers(reassessmentTriggers);
        }
    }
}
