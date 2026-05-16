package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.repository.ObservationRepository;
import com.keplerops.groundcontrol.domain.assets.repository.OperationalAssetRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.findings.repository.FindingRepository;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.graph.service.GraphTargetResolverService;
import com.keplerops.groundcontrol.domain.requirements.repository.RequirementRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.MethodologyProfileRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskAssessmentResultRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskRegisterRecordRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.TreatmentPlanRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelRepository;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType;
import com.keplerops.groundcontrol.domain.verification.repository.VerificationResultRepository;
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

    @Mock
    private ThreatModelRepository threatModelRepository;

    @Mock
    private VerificationResultRepository verificationResultRepository;

    @Mock
    private FindingRepository findingRepository;

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
                "CONTROL",
                "THREAT_MODEL_ENTRY",
                "FINDING"
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
            names = {"EVIDENCE", "AUDIT", "ISSUE", "CODE", "CONFIGURATION", "EXTERNAL"})
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
                "CONTROL",
                "THREAT_MODEL",
                "FINDING"
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
            names = {"VULNERABILITY", "EVIDENCE", "AUDIT_RECORD", "EXTERNAL"})
    void validateRiskScenarioTargetAcceptsExternalTargets(RiskScenarioLinkTargetType targetType) {
        var validated = graphTargetResolverService.validateRiskScenarioTarget(projectId, targetType, null, "EXT-2");

        assertThat(validated.internal()).isFalse();
        assertThat(validated.targetIdentifier()).isEqualTo("EXT-2");
    }

    @ParameterizedTest
    @EnumSource(
            value = ThreatModelLinkTargetType.class,
            names = {
                "ASSET",
                "REQUIREMENT",
                "CONTROL",
                "RISK_SCENARIO",
                "OBSERVATION",
                "RISK_ASSESSMENT_RESULT",
                "VERIFICATION_RESULT",
                "FINDING"
            })
    void validateThreatModelTargetAcceptsInternalTargets(ThreatModelLinkTargetType targetType) {
        stubThreatModelInternalTarget(targetType, true);

        var validated = graphTargetResolverService.validateThreatModelTarget(projectId, targetType, targetId, null);

        assertThat(validated.internal()).isTrue();
        assertThat(validated.targetEntityId()).isEqualTo(targetId);
    }

    @ParameterizedTest
    @EnumSource(
            value = ThreatModelLinkTargetType.class,
            names = {"ARCHITECTURE_MODEL", "CODE", "ISSUE", "EVIDENCE", "EXTERNAL"})
    void validateThreatModelTargetAcceptsExternalTargets(ThreatModelLinkTargetType targetType) {
        var validated =
                graphTargetResolverService.validateThreatModelTarget(projectId, targetType, null, "backend/Auth.java");

        assertThat(validated.internal()).isFalse();
        assertThat(validated.targetEntityId()).isNull();
        assertThat(validated.targetIdentifier()).isEqualTo("backend/Auth.java");
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

    @Test
    void validateAssetTargetRejectsMissingThreatModelEntry() {
        when(threatModelRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateAssetTarget(
                        projectId, AssetLinkTargetType.THREAT_MODEL_ENTRY, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Threat model");
    }

    @Test
    void validateRiskScenarioTargetRejectsMissingThreatModel() {
        when(threatModelRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateRiskScenarioTarget(
                        projectId, RiskScenarioLinkTargetType.THREAT_MODEL, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Threat model");
    }

    @Test
    void validateThreatModelTargetRejectsMissingInternalTargetEntityId() {
        assertThatThrownBy(() -> graphTargetResolverService.validateThreatModelTarget(
                        projectId, ThreatModelLinkTargetType.ASSET, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("targetEntityId");
    }

    @Test
    void validateThreatModelTargetRejectsMissingProjectScopedTarget() {
        when(assetRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateThreatModelTarget(
                        projectId, ThreatModelLinkTargetType.ASSET, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void validateThreatModelTargetRejectsMissingExternalIdentifier() {
        assertThatThrownBy(() -> graphTargetResolverService.validateThreatModelTarget(
                        projectId, ThreatModelLinkTargetType.EXTERNAL, null, " "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("targetIdentifier");
    }

    @Test
    void validateThreatModelTargetRejectsMissingFinding() {
        when(findingRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateThreatModelTarget(
                        projectId, ThreatModelLinkTargetType.FINDING, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Finding");
    }

    @Test
    void validateAssetTargetRejectsMissingFinding() {
        when(findingRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateAssetTarget(
                        projectId, AssetLinkTargetType.FINDING, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Finding");
    }

    @Test
    void validateControlTargetRejectsMissingFinding() {
        when(findingRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateControlTarget(
                        projectId, ControlLinkTargetType.FINDING, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Finding");
    }

    @Test
    void validateRiskScenarioTargetRejectsMissingFinding() {
        when(findingRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateRiskScenarioTarget(
                        projectId, RiskScenarioLinkTargetType.FINDING, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Finding");
    }

    @ParameterizedTest
    @EnumSource(
            value = ControlLinkTargetType.class,
            names = {
                "ASSET",
                "REQUIREMENT",
                "RISK_SCENARIO",
                "RISK_REGISTER_RECORD",
                "RISK_ASSESSMENT_RESULT",
                "TREATMENT_PLAN",
                "METHODOLOGY_PROFILE",
                "OBSERVATION",
                "FINDING"
            })
    void validateControlTargetAcceptsInternalTargets(ControlLinkTargetType targetType) {
        stubControlInternalTarget(targetType, true);

        var validated = graphTargetResolverService.validateControlTarget(projectId, targetType, targetId, null);

        assertThat(validated.internal()).isTrue();
        assertThat(validated.targetEntityId()).isEqualTo(targetId);
    }

    @ParameterizedTest
    @EnumSource(
            value = ControlLinkTargetType.class,
            names = {"EVIDENCE", "CODE", "CONFIGURATION", "OPERATIONAL_ARTIFACT", "EXTERNAL"})
    void validateControlTargetAcceptsExternalTargets(ControlLinkTargetType targetType) {
        var validated = graphTargetResolverService.validateControlTarget(projectId, targetType, null, "ref://ext/1");

        assertThat(validated.internal()).isFalse();
        assertThat(validated.targetEntityId()).isNull();
        assertThat(validated.targetIdentifier()).isEqualTo("ref://ext/1");
    }

    @Test
    void validateControlTargetRejectsMissingInternalTargetEntityId() {
        assertThatThrownBy(() -> graphTargetResolverService.validateControlTarget(
                        projectId, ControlLinkTargetType.ASSET, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("targetEntityId");
    }

    @Test
    void validateControlTargetRejectsCrossProjectInternalTarget() {
        // Issue #875: an attacker passing a UUID from project B with the
        // current control's project A used to silently persist a cross-project
        // edge. The resolver now enforces project-scoped existence.
        when(requirementRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateControlTarget(
                        projectId, ControlLinkTargetType.REQUIREMENT, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void validateControlTargetRejectsMissingExternalIdentifier() {
        assertThatThrownBy(() -> graphTargetResolverService.validateControlTarget(
                        projectId, ControlLinkTargetType.EXTERNAL, null, " "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("targetIdentifier");
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
            case THREAT_MODEL_ENTRY -> when(threatModelRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case FINDING -> when(findingRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case EVIDENCE, AUDIT, ISSUE, CODE, CONFIGURATION, EXTERNAL -> throw new IllegalArgumentException(
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
            case THREAT_MODEL -> when(threatModelRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case FINDING -> when(findingRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case VULNERABILITY, EVIDENCE, AUDIT_RECORD, EXTERNAL -> throw new IllegalArgumentException(
                    "Not an internal target type");
        }
    }

    private void stubThreatModelInternalTarget(ThreatModelLinkTargetType targetType, boolean exists) {
        switch (targetType) {
            case ASSET -> when(assetRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case REQUIREMENT -> when(requirementRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case CONTROL -> when(controlRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case RISK_SCENARIO -> when(riskScenarioRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case OBSERVATION -> when(observationRepository.findByIdWithAssetAndProjectId(targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.assets.model.Observation.class))
                                    : java.util.Optional.empty());
            case RISK_ASSESSMENT_RESULT -> when(riskAssessmentResultRepository.findByIdAndProjectIdWithObservations(
                            targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.riskscenarios.model.RiskAssessmentResult
                                                    .class))
                                    : java.util.Optional.empty());
            case VERIFICATION_RESULT -> when(verificationResultRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case FINDING -> when(findingRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case ARCHITECTURE_MODEL, CODE, ISSUE, EVIDENCE, EXTERNAL -> throw new IllegalArgumentException(
                    "Not an internal target type");
        }
    }

    private void stubControlInternalTarget(ControlLinkTargetType targetType, boolean exists) {
        switch (targetType) {
            case ASSET -> when(assetRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
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
            case OBSERVATION -> when(observationRepository.findByIdWithAssetAndProjectId(targetId, projectId))
                    .thenReturn(
                            exists
                                    ? java.util.Optional.of(org.mockito.Mockito.mock(
                                            com.keplerops.groundcontrol.domain.assets.model.Observation.class))
                                    : java.util.Optional.empty());
            case FINDING -> when(findingRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case EVIDENCE, CODE, CONFIGURATION, OPERATIONAL_ARTIFACT, EXTERNAL -> throw new IllegalArgumentException(
                    "Not an internal target type");
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = FindingLinkTargetType.class,
            names = {"CONTROL", "RISK_SCENARIO", "ASSET", "OBSERVATION"})
    void validateFindingTargetAcceptsInternalTargets(FindingLinkTargetType targetType) {
        stubFindingInternalTarget(targetType, true);

        var validated = graphTargetResolverService.validateFindingTarget(projectId, targetType, targetId, null);

        assertThat(validated.internal()).isTrue();
        assertThat(validated.targetEntityId()).isEqualTo(targetId);
    }

    @ParameterizedTest
    @EnumSource(
            value = FindingLinkTargetType.class,
            names = {"OPERATIONAL_ARTIFACT", "EVIDENCE", "AUDIT", "REMEDIATION_PLAN", "EXTERNAL"})
    void validateFindingTargetAcceptsExternalTargets(FindingLinkTargetType targetType) {
        var validated = graphTargetResolverService.validateFindingTarget(projectId, targetType, null, "EXT-F");

        assertThat(validated.internal()).isFalse();
        assertThat(validated.targetEntityId()).isNull();
        assertThat(validated.targetIdentifier()).isEqualTo("EXT-F");
    }

    @Test
    void validateFindingTargetRejectsMissingExternalIdentifier() {
        assertThatThrownBy(() -> graphTargetResolverService.validateFindingTarget(
                        projectId, FindingLinkTargetType.EXTERNAL, null, " "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("targetIdentifier");
    }

    @Test
    void validateFindingTargetRejectsMissingInternalTargetEntityId() {
        assertThatThrownBy(() -> graphTargetResolverService.validateFindingTarget(
                        projectId, FindingLinkTargetType.CONTROL, null, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("targetEntityId");
    }

    @Test
    void validateFindingTargetRejectsCrossProjectInternalTarget() {
        when(controlRepository.existsByIdAndProjectId(targetId, projectId)).thenReturn(false);

        assertThatThrownBy(() -> graphTargetResolverService.validateFindingTarget(
                        projectId, FindingLinkTargetType.CONTROL, targetId, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("not found");
    }

    private void stubFindingInternalTarget(FindingLinkTargetType targetType, boolean exists) {
        switch (targetType) {
            case CONTROL -> when(controlRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case RISK_SCENARIO -> when(riskScenarioRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case ASSET -> when(assetRepository.existsByIdAndProjectId(targetId, projectId))
                    .thenReturn(exists);
            case OBSERVATION -> when(observationRepository.findByIdWithAssetAndProjectId(targetId, projectId))
                    .thenReturn(java.util.Optional.of(org.mockito.Mockito.mock(
                            com.keplerops.groundcontrol.domain.assets.model.Observation.class)));
            case OPERATIONAL_ARTIFACT,
                    EVIDENCE,
                    AUDIT,
                    REMEDIATION_PLAN,
                    EXTERNAL -> throw new IllegalArgumentException("Not an internal target type");
        }
    }
}
