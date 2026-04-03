package com.keplerops.groundcontrol.domain.graph.service;

import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.MethodologyProfileRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskAssessmentResultRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskRegisterRecordRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.TreatmentPlanRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GraphTargetResolverService {

    public record ValidatedTarget(UUID targetEntityId, String targetIdentifier, boolean internal) {}

    private final RequirementRepository requirementRepository;
    private final OperationalAssetRepository assetRepository;
    private final ObservationRepository observationRepository;
    private final RiskScenarioRepository riskScenarioRepository;
    private final RiskRegisterRecordRepository riskRegisterRecordRepository;
    private final RiskAssessmentResultRepository riskAssessmentResultRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final MethodologyProfileRepository methodologyProfileRepository;

    public GraphTargetResolverService(
            RequirementRepository requirementRepository,
            OperationalAssetRepository assetRepository,
            ObservationRepository observationRepository,
            RiskScenarioRepository riskScenarioRepository,
            RiskRegisterRecordRepository riskRegisterRecordRepository,
            RiskAssessmentResultRepository riskAssessmentResultRepository,
            TreatmentPlanRepository treatmentPlanRepository,
            MethodologyProfileRepository methodologyProfileRepository) {
        this.requirementRepository = requirementRepository;
        this.assetRepository = assetRepository;
        this.observationRepository = observationRepository;
        this.riskScenarioRepository = riskScenarioRepository;
        this.riskRegisterRecordRepository = riskRegisterRecordRepository;
        this.riskAssessmentResultRepository = riskAssessmentResultRepository;
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.methodologyProfileRepository = methodologyProfileRepository;
    }

    public ValidatedTarget validateAssetTarget(
            UUID projectId, AssetLinkTargetType targetType, UUID targetEntityId, String targetIdentifier) {
        return switch (targetType) {
            case REQUIREMENT -> internalTarget(
                    targetEntityId,
                    requirementRepository.existsByIdAndProjectId(targetEntityId, projectId),
                    "Requirement");
            case RISK_SCENARIO -> internalTarget(
                    targetEntityId,
                    riskScenarioRepository.existsByIdAndProjectId(targetEntityId, projectId),
                    "Risk scenario");
            case RISK_REGISTER_RECORD -> internalTarget(
                    targetEntityId,
                    riskRegisterRecordRepository
                            .findByIdAndProjectIdWithScenarios(targetEntityId, projectId)
                            .isPresent(),
                    "Risk register record");
            case RISK_ASSESSMENT_RESULT -> internalTarget(
                    targetEntityId,
                    riskAssessmentResultRepository
                            .findByIdAndProjectIdWithObservations(targetEntityId, projectId)
                            .isPresent(),
                    "Risk assessment result");
            case TREATMENT_PLAN -> internalTarget(
                    targetEntityId,
                    treatmentPlanRepository
                            .findByIdAndProjectId(targetEntityId, projectId)
                            .isPresent(),
                    "Treatment plan");
            case METHODOLOGY_PROFILE -> internalTarget(
                    targetEntityId,
                    methodologyProfileRepository
                            .findByIdAndProjectId(targetEntityId, projectId)
                            .isPresent(),
                    "Methodology profile");
            case CONTROL, THREAT_MODEL_ENTRY, FINDING, EVIDENCE, AUDIT, EXTERNAL -> externalTarget(targetIdentifier);
        };
    }

    public ValidatedTarget validateRiskScenarioTarget(
            UUID projectId, RiskScenarioLinkTargetType targetType, UUID targetEntityId, String targetIdentifier) {
        return switch (targetType) {
            case OBSERVATION -> internalTarget(
                    targetEntityId,
                    observationRepository
                            .findByIdWithAssetAndProjectId(targetEntityId, projectId)
                            .isPresent(),
                    "Observation");
            case ASSET -> internalTarget(
                    targetEntityId, assetRepository.existsByIdAndProjectId(targetEntityId, projectId), "Asset");
            case REQUIREMENT -> internalTarget(
                    targetEntityId,
                    requirementRepository.existsByIdAndProjectId(targetEntityId, projectId),
                    "Requirement");
            case RISK_REGISTER_RECORD -> internalTarget(
                    targetEntityId,
                    riskRegisterRecordRepository
                            .findByIdAndProjectIdWithScenarios(targetEntityId, projectId)
                            .isPresent(),
                    "Risk register record");
            case RISK_ASSESSMENT_RESULT -> internalTarget(
                    targetEntityId,
                    riskAssessmentResultRepository
                            .findByIdAndProjectIdWithObservations(targetEntityId, projectId)
                            .isPresent(),
                    "Risk assessment result");
            case TREATMENT_PLAN -> internalTarget(
                    targetEntityId,
                    treatmentPlanRepository
                            .findByIdAndProjectId(targetEntityId, projectId)
                            .isPresent(),
                    "Treatment plan");
            case METHODOLOGY_PROFILE -> internalTarget(
                    targetEntityId,
                    methodologyProfileRepository
                            .findByIdAndProjectId(targetEntityId, projectId)
                            .isPresent(),
                    "Methodology profile");
            case THREAT_MODEL, VULNERABILITY, CONTROL, FINDING, EVIDENCE, AUDIT_RECORD, EXTERNAL -> externalTarget(
                    targetIdentifier);
        };
    }

    private ValidatedTarget internalTarget(UUID targetEntityId, boolean exists, String label) {
        if (targetEntityId == null) {
            throw new DomainValidationException(label + " links require targetEntityId");
        }
        if (!exists) {
            throw new DomainValidationException(label + " target not found in the requested project");
        }
        return new ValidatedTarget(targetEntityId, null, true);
    }

    private ValidatedTarget externalTarget(String targetIdentifier) {
        if (targetIdentifier == null || targetIdentifier.isBlank()) {
            throw new DomainValidationException("External or unmodeled links require targetIdentifier");
        }
        return new ValidatedTarget(null, targetIdentifier, false);
    }
}
