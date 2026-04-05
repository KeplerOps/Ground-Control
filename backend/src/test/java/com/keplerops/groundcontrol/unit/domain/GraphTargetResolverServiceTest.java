package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.graph.service.GraphTargetResolverService;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.MethodologyProfileRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskAssessmentResultRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskRegisterRecordRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.TreatmentPlanRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraphTargetResolverServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private OperationalAssetRepository assetRepository;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private RiskScenarioRepository riskScenarioRepository;

    @Mock
    private RiskRegisterRecordRepository riskRegisterRecordRepository;

    @Mock
    private RiskAssessmentResultRepository riskAssessmentResultRepository;

    @Mock
    private TreatmentPlanRepository treatmentPlanRepository;

    @Mock
    private MethodologyProfileRepository methodologyProfileRepository;

    @Mock
    private com.keplerops.groundcontrol.domain.controls.repository.ControlRepository controlRepository;

    @InjectMocks
    private GraphTargetResolverService graphTargetResolverService;

    private final UUID projectId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @ParameterizedTest
    @EnumSource(
            value = AssetLinkTargetType.class,
            names = {
                "REQUIREMENT",
                "RISK_SCENARIO",
                "RISK_REGISTER_RECORD",
                "RISK_ASSESSMENT_RESULT",
                "TREATMENT_PLAN",
                "METHODOLOGY_PROFILE",
                "CONTROL"
            })
    void validateAssetTargetAcceptsInternalTargets(AssetLinkTargetType targetType) {
        stubAssetInternalTarget(targetType, true);

        var validated = graphTargetResolverService.validateAssetTarget(projectId, targetType, targetId, null);

        assertThat(validated.internal()).isTrue();
        assertThat(validated.targetEntityId()).isEqualTo(targetId);
        assertThat(validated.targetIdentifier()).isNull();
    }

    @ParameterizedTest
    @EnumSource(
            value = AssetLinkTargetType.class,
            names = {"THREAT_MODEL_ENTRY", "FINDING", "EVIDENCE", "AUDIT", "EXTERNAL"})
    void validateAssetTargetAcceptsExternalTargets(AssetLinkTargetType targetType) {
        var validated = graphTargetResolverService.validateAssetTarget(projectId, targetType, null, "EXT-1");

        assertThat(validated.internal()).isFalse();
        assertThat(validated.targetEntityId()).isNull();
        assertThat(validated.targetIdentifier()).isEqualTo("EXT-1");
    }

    @ParameterizedTest
    @EnumSource(
            value = RiskScenarioLinkTargetType.class,
            names = {
                "OBSERVATION",
                "ASSET",
                "REQUIREMENT",
                "RISK_REGISTER_RECORD",
                "RISK_ASSESSMENT_RESULT",
                "TREATMENT_PLAN",
                "METHODOLOGY_PROFILE",
                "CONTROL"
            })
    void validateRiskScenarioTargetAcceptsInternalTargets(RiskScenarioLinkTargetType targetType) {
        stubScenarioInternalTarget(targetType, true);

        var validated = graphTargetResolverService.validateRiskScenarioTarget(projectId, targetType, targetId, null);

        assertThat(validated.internal()).isTrue();
        assertThat(validated.targetEntityId()).isEqualTo(targetId);
    }

    @ParameterizedTest
    @EnumSource(
            value = RiskScenarioLinkTargetType.class,
            names = {"THREAT_MODEL", "VULNERABILITY", "FINDING", "EVIDENCE", "AUDIT_RECORD", "EXTERNAL"})
    void validateRiskScenarioTargetAcceptsExternalTargets(RiskScenarioLinkTargetType targetType) {
        var validated = graphTargetResolverService.validateRiskScenarioTarget(projectId, targetType, null, "EXT-2");

        assertThat(validated.internal()).isFalse();
        assertThat(validated.targetIdentifier()).isEqualTo("EXT-2");
    }

    @Test
    void validateAssetTargetRejectsMissingInternalTargetEntityId() {
        assertThatThrownBy(() -> graphTargetResolverService.validateAssetTarget(
                        projectId, AssetLinkTargetType.REQUIREMENT, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("targetEntityId");
    }

    @Test
    void validateRiskScenarioTargetRejectsMissingExternalIdentifier() {
        assertThatThrownBy(() -> graphTargetResolverService.validateRiskScenarioTarget(
                        projectId, RiskScenarioLinkTargetType.EXTERNAL, null, " "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("targetIdentifier");
    }

    @Test
    void validateAssetTargetRejectsMissingProjectScopedTarget() {
        when(requirementRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateAssetTarget(
                        projectId, AssetLinkTargetType.REQUIREMENT, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void validateRiskScenarioTargetRejectsMissingProjectScopedTarget() {
        when(assetRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateRiskScenarioTarget(
                        projectId, RiskScenarioLinkTargetType.ASSET, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("not found");
    }

    private void stubAssetInternalTarget(AssetLinkTargetType targetType, boolean exists) {
        switch (targetType) {
            case REQUIREMENT -> when(requirementRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case RISK_SCENARIO -> when(riskScenarioRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case RISK_REGISTER_RECORD -> when(riskRegisterRecordRepository.findByIdAndProjectIdWithScenarios(
                            targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.riskscenarios.model.RiskRegisterRecord
                                                    .class))
                                    : java.util.Optional.empty());
            case RISK_ASSESSMENT_RESULT -> when(riskAssessmentResultRepository.findByIdAndProjectIdWithObservations(
                            targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.riskscenarios.model.RiskAssessmentResult
                                                    .class))
                                    : java.util.Optional.empty());
            case TREATMENT_PLAN -> when(treatmentPlanRepository.findByIdAndProjectId(targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.riskscenarios.model.TreatmentPlan.class))
                                    : java.util.Optional.empty());
            case METHODOLOGY_PROFILE -> when(methodologyProfileRepository.findByIdAndProjectId(targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.riskscenarios.model.MethodologyProfile
                                                    .class))
                                    : java.util.Optional.empty());
            case CONTROL -> when(controlRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case THREAT_MODEL_ENTRY, FINDING, EVIDENCE, AUDIT, EXTERNAL -> throw new IllegalArgumentException(
                    "Not an internal target type");
        }
    }

    private void stubScenarioInternalTarget(RiskScenarioLinkTargetType targetType, boolean exists) {
        switch (targetType) {
            case OBSERVATION -> when(observationRepository.findByIdWithAssetAndProjectId(targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.assets.model.Observation.class))
                                    : java.util.Optional.empty());
            case ASSET -> when(assetRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case REQUIREMENT -> when(requirementRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case RISK_REGISTER_RECORD -> when(riskRegisterRecordRepository.findByIdAndProjectIdWithScenarios(
                            targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.riskscenarios.model.RiskRegisterRecord
                                                    .class))
                                    : java.util.Optional.empty());
            case RISK_ASSESSMENT_RESULT -> when(riskAssessmentResultRepository.findByIdAndProjectIdWithObservations(
                            targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.riskscenarios.model.RiskAssessmentResult
                                                    .class))
                                    : java.util.Optional.empty());
            case TREATMENT_PLAN -> when(treatmentPlanRepository.findByIdAndProjectId(targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.riskscenarios.model.TreatmentPlan.class))
                                    : java.util.Optional.empty());
            case METHODOLOGY_PROFILE -> when(methodologyProfileRepository.findByIdAndProjectId(targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.riskscenarios.model.MethodologyProfile
                                                    .class))
                                    : java.util.Optional.empty());
            case CONTROL -> when(controlRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case THREAT_MODEL,
                    VULNERABILITY,
                    FINDING,
                    EVIDENCE,
                    AUDIT_RECORD,
                    EXTERNAL -> throw new IllegalArgumentException("Not an internal target type");
        }
    }
}
