package com.keplerops.groundcontrol.domain.riskscenarios.service;

import com.keplerops.groundcontrol.domain.assets.model.Observation;
import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.model.MethodologyProfile;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskAssessmentResult;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskRegisterRecord;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.MethodologyProfileRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskAssessmentResultRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskRegisterRecordRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskAssessmentApprovalStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RiskAssessmentResultService {

    private final RiskAssessmentResultRepository repository;
    private final RiskScenarioRepository riskScenarioRepository;
    private final MethodologyProfileRepository methodologyProfileRepository;
    private final RiskRegisterRecordRepository riskRegisterRecordRepository;
    private final ObservationRepository observationRepository;
    private final ProjectService projectService;

    public RiskAssessmentResultService(
            RiskAssessmentResultRepository repository,
            RiskScenarioRepository riskScenarioRepository,
            MethodologyProfileRepository methodologyProfileRepository,
            RiskRegisterRecordRepository riskRegisterRecordRepository,
            ObservationRepository observationRepository,
            ProjectService projectService) {
        this.repository = repository;
        this.riskScenarioRepository = riskScenarioRepository;
        this.methodologyProfileRepository = methodologyProfileRepository;
        this.riskRegisterRecordRepository = riskRegisterRecordRepository;
        this.observationRepository = observationRepository;
        this.projectService = projectService;
    }

    public RiskAssessmentResult create(CreateRiskAssessmentResultCommand command) {
        var project = projectService.getById(command.projectId());
        var scenario = getScenario(project.getId(), command.riskScenarioId());
        var profile = getProfile(project.getId(), command.methodologyProfileId());
        var result = new RiskAssessmentResult(project, scenario, profile);
        applyUpdates(result, project.getId(), command);
        return repository.save(result);
    }

    @Transactional(readOnly = true)
    public List<RiskAssessmentResult> listByProject(UUID projectId) {
        return repository.findByProjectIdWithObservationsOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<RiskAssessmentResult> listByScenario(UUID projectId, UUID riskScenarioId) {
        getScenario(projectId, riskScenarioId);
        return repository.findByProjectIdAndRiskScenarioIdOrderByCreatedAtDesc(projectId, riskScenarioId);
    }

    @Transactional(readOnly = true)
    public List<RiskAssessmentResult> listByRiskRegisterRecord(UUID projectId, UUID riskRegisterRecordId) {
        getRiskRegisterRecord(projectId, riskRegisterRecordId);
        return repository.findByProjectIdAndRiskRegisterRecordIdOrderByCreatedAtDesc(projectId, riskRegisterRecordId);
    }

    @Transactional(readOnly = true)
    public RiskAssessmentResult getById(UUID projectId, UUID id) {
        return repository
                .findByIdAndProjectIdWithObservations(id, projectId)
                .orElseThrow(() -> new NotFoundException("Risk assessment result not found: " + id));
    }

    public RiskAssessmentResult update(UUID projectId, UUID id, UpdateRiskAssessmentResultCommand command) {
        var result = getById(projectId, id);
        applyUpdates(result, projectId, command);
        return repository.save(result);
    }

    public RiskAssessmentResult transitionApprovalState(
            UUID projectId, UUID id, RiskAssessmentApprovalStatus approvalState) {
        var result = getById(projectId, id);
        result.transitionApprovalState(approvalState);
        return repository.save(result);
    }

    public void delete(UUID projectId, UUID id) {
        repository.delete(getById(projectId, id));
    }

    private void applyUpdates(RiskAssessmentResult result, UUID projectId, CreateRiskAssessmentResultCommand command) {
        applySharedUpdates(
                result,
                projectId,
                command.riskRegisterRecordId(),
                command.methodologyProfileId(),
                command.analystIdentity(),
                command.assumptions(),
                command.inputFactors(),
                command.observationDate(),
                command.assessmentAt(),
                command.timeHorizon(),
                command.confidence(),
                command.uncertaintyMetadata(),
                command.computedOutputs(),
                command.evidenceRefs(),
                command.notes(),
                command.observationIds());
    }

    private void applyUpdates(RiskAssessmentResult result, UUID projectId, UpdateRiskAssessmentResultCommand command) {
        applySharedUpdates(
                result,
                projectId,
                command.riskRegisterRecordId(),
                command.methodologyProfileId(),
                command.analystIdentity(),
                command.assumptions(),
                command.inputFactors(),
                command.observationDate(),
                command.assessmentAt(),
                command.timeHorizon(),
                command.confidence(),
                command.uncertaintyMetadata(),
                command.computedOutputs(),
                command.evidenceRefs(),
                command.notes(),
                command.observationIds());
    }

    private void applySharedUpdates(
            RiskAssessmentResult result,
            UUID projectId,
            UUID riskRegisterRecordId,
            UUID methodologyProfileId,
            String analystIdentity,
            String assumptions,
            java.util.Map<String, Object> inputFactors,
            java.time.Instant observationDate,
            java.time.Instant assessmentAt,
            String timeHorizon,
            String confidence,
            java.util.Map<String, Object> uncertaintyMetadata,
            java.util.Map<String, Object> computedOutputs,
            List<String> evidenceRefs,
            String notes,
            List<UUID> observationIds) {
        if (methodologyProfileId != null) {
            result.setMethodologyProfile(getProfile(projectId, methodologyProfileId));
        }
        if (riskRegisterRecordId != null) {
            var record = getRiskRegisterRecord(projectId, riskRegisterRecordId);
            if (!record.getRiskScenarios().isEmpty()
                    && record.getRiskScenarios().stream().noneMatch(scenario -> scenario.getId()
                            .equals(result.getRiskScenario().getId()))) {
                throw new DomainValidationException(
                        "Risk register record " + record.getUid() + " is not linked to scenario "
                                + result.getRiskScenario().getUid());
            }
            result.setRiskRegisterRecord(record);
        }
        if (analystIdentity != null) {
            result.setAnalystIdentity(analystIdentity);
        }
        if (assumptions != null) {
            result.setAssumptions(assumptions);
        }
        if (inputFactors != null) {
            result.setInputFactors(inputFactors);
        }
        if (observationDate != null) {
            result.setObservationDate(observationDate);
        }
        if (assessmentAt != null) {
            result.setAssessmentAt(assessmentAt);
        }
        if (timeHorizon != null) {
            result.setTimeHorizon(timeHorizon);
        }
        if (confidence != null) {
            result.setConfidence(confidence);
        }
        if (uncertaintyMetadata != null) {
            result.setUncertaintyMetadata(uncertaintyMetadata);
        }
        if (computedOutputs != null) {
            result.setComputedOutputs(computedOutputs);
        }
        if (evidenceRefs != null) {
            result.setEvidenceRefs(evidenceRefs);
        }
        if (notes != null) {
            result.setNotes(notes);
        }
        if (observationIds != null) {
            result.replaceObservations(resolveObservations(projectId, observationIds));
        }
    }

    private RiskScenario getScenario(UUID projectId, UUID scenarioId) {
        return riskScenarioRepository
                .findByIdAndProjectId(scenarioId, projectId)
                .orElseThrow(() -> new NotFoundException("Risk scenario not found: " + scenarioId));
    }

    private MethodologyProfile getProfile(UUID projectId, UUID profileId) {
        return methodologyProfileRepository
                .findByIdAndProjectId(profileId, projectId)
                .orElseThrow(() -> new NotFoundException("Methodology profile not found: " + profileId));
    }

    private RiskRegisterRecord getRiskRegisterRecord(UUID projectId, UUID recordId) {
        return riskRegisterRecordRepository
                .findByIdAndProjectIdWithScenarios(recordId, projectId)
                .orElseThrow(() -> new NotFoundException("Risk register record not found: " + recordId));
    }

    private List<Observation> resolveObservations(UUID projectId, List<UUID> observationIds) {
        if (observationIds.isEmpty()) {
            return List.of();
        }
        var observations = observationRepository.findAllByIdInAndProjectId(observationIds, projectId);
        if (observations.size() != observationIds.size()) {
            throw new DomainValidationException("One or more observations do not belong to the requested project");
        }
        return observations;
    }
}
