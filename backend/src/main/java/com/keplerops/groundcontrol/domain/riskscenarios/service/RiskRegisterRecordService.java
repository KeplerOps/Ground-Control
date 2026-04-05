package com.keplerops.groundcontrol.domain.riskscenarios.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskRegisterRecord;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskRegisterRecordRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskRegisterStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RiskRegisterRecordService {

    private final RiskRegisterRecordRepository repository;
    private final RiskScenarioRepository riskScenarioRepository;
    private final ProjectService projectService;

    public RiskRegisterRecordService(
            RiskRegisterRecordRepository repository,
            RiskScenarioRepository riskScenarioRepository,
            ProjectService projectService) {
        this.repository = repository;
        this.riskScenarioRepository = riskScenarioRepository;
        this.projectService = projectService;
    }

    public RiskRegisterRecord create(CreateRiskRegisterRecordCommand command) {
        var project = projectService.getById(command.projectId());
        if (repository.existsByProjectIdAndUid(project.getId(), command.uid())) {
            throw new ConflictException("Risk register record with UID " + command.uid() + " already exists");
        }
        var record = new RiskRegisterRecord(project, command.uid(), command.title());
        applyUpdates(
                record,
                command.owner(),
                command.reviewCadence(),
                command.nextReviewAt(),
                command.categoryTags(),
                command.decisionMetadata(),
                command.assetScopeSummary(),
                resolveScenarios(project.getId(), command.riskScenarioIds()));
        return repository.save(record);
    }

    @Transactional(readOnly = true)
    public List<RiskRegisterRecord> listByProject(UUID projectId) {
        return repository.findByProjectIdWithScenariosOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public RiskRegisterRecord getById(UUID projectId, UUID id) {
        return repository
                .findByIdAndProjectIdWithScenarios(id, projectId)
                .orElseThrow(() -> new NotFoundException("Risk register record not found: " + id));
    }

    public RiskRegisterRecord update(UUID projectId, UUID id, UpdateRiskRegisterRecordCommand command) {
        var record = getById(projectId, id);
        if (command.title() != null) {
            record.setTitle(command.title());
        }
        applyUpdates(
                record,
                command.owner(),
                command.reviewCadence(),
                command.nextReviewAt(),
                command.categoryTags(),
                command.decisionMetadata(),
                command.assetScopeSummary(),
                command.riskScenarioIds() != null ? resolveScenarios(projectId, command.riskScenarioIds()) : null);
        return repository.save(record);
    }

    public RiskRegisterRecord transitionStatus(UUID projectId, UUID id, RiskRegisterStatus status) {
        var record = getById(projectId, id);
        record.transitionStatus(status);
        return repository.save(record);
    }

    public void delete(UUID projectId, UUID id) {
        repository.delete(getById(projectId, id));
    }

    private List<RiskScenario> resolveScenarios(UUID projectId, List<UUID> ids) {
        if (ids == null) {
            return List.of();
        }
        var scenarios = riskScenarioRepository.findByIdInAndProjectId(ids, projectId);
        if (scenarios.size() != ids.size()) {
            throw new DomainValidationException("One or more risk scenarios do not belong to the requested project");
        }
        return scenarios;
    }

    private void applyUpdates(
            RiskRegisterRecord record,
            String owner,
            String reviewCadence,
            java.time.Instant nextReviewAt,
            List<String> categoryTags,
            java.util.Map<String, Object> decisionMetadata,
            String assetScopeSummary,
            List<RiskScenario> scenarios) {
        if (owner != null) {
            record.setOwner(owner);
        }
        if (reviewCadence != null) {
            record.setReviewCadence(reviewCadence);
        }
        if (nextReviewAt != null) {
            record.setNextReviewAt(nextReviewAt);
        }
        if (categoryTags != null) {
            record.setCategoryTags(categoryTags);
        }
        if (decisionMetadata != null) {
            record.setDecisionMetadata(decisionMetadata);
        }
        if (assetScopeSummary != null) {
            record.setAssetScopeSummary(assetScopeSummary);
        }
        if (scenarios != null) {
            record.replaceRiskScenarios(scenarios);
        }
    }
}
