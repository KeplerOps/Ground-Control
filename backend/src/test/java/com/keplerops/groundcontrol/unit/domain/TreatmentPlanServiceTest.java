package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskRegisterRecord;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.model.TreatmentPlan;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskRegisterRecordRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.TreatmentPlanRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.service.CreateTreatmentPlanCommand;
import com.keplerops.groundcontrol.domain.riskscenarios.service.TreatmentPlanService;
import com.keplerops.groundcontrol.domain.riskscenarios.service.UpdateTreatmentPlanCommand;
import com.keplerops.groundcontrol.domain.riskscenarios.state.TreatmentPlanStatus;
import com.keplerops.groundcontrol.domain.riskscenarios.state.TreatmentStrategy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TreatmentPlanServiceTest {

    @Mock
    private TreatmentPlanRepository repository;

    @Mock
    private RiskRegisterRecordRepository riskRegisterRecordRepository;

    @Mock
    private RiskScenarioRepository riskScenarioRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private TreatmentPlanService service;

    private Project project;
    private UUID projectId;
    private RiskScenario scenario;
    private UUID scenarioId;
    private RiskRegisterRecord record;
    private UUID recordId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
        scenario = new RiskScenario(project, "RS-1", "Gateway risk", "Actor", "Exploit", "Gateway", "Outage");
        scenario.setTimeHorizon("12 months");
        scenarioId = UUID.randomUUID();
        setField(scenario, "id", scenarioId);
        record = new RiskRegisterRecord(project, "RR-1", "Gateway record");
        record.replaceRiskScenarios(List.of(scenario));
        recordId = UUID.randomUUID();
        setField(record, "id", recordId);
    }

    @Test
    void createBuildsPlanAndTransitionsRequestedStatus() {
        when(projectService.getById(projectId)).thenReturn(project);
        when(repository.existsByProjectIdAndUid(projectId, "TP-1")).thenReturn(false);
        when(riskRegisterRecordRepository.findByIdAndProjectIdWithScenarios(recordId, projectId))
                .thenReturn(Optional.of(record));
        when(riskScenarioRepository.findByIdAndProjectId(scenarioId, projectId)).thenReturn(Optional.of(scenario));
        when(repository.save(any(TreatmentPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(new CreateTreatmentPlanCommand(
                projectId,
                "TP-1",
                "Mitigate gateway",
                recordId,
                scenarioId,
                TreatmentStrategy.MITIGATE,
                "Owner",
                "Rationale",
                Instant.parse("2026-06-01T00:00:00Z"),
                TreatmentPlanStatus.IN_PROGRESS,
                List.of(Map.of("step", "Enable WAF")),
                List.of("New exposure")));

        assertThat(result.getUid()).isEqualTo("TP-1");
        assertThat(result.getStatus()).isEqualTo(TreatmentPlanStatus.IN_PROGRESS);
        assertThat(result.getRiskScenario()).isSameAs(scenario);
        assertThat(result.getActionItems()).hasSize(1);
    }

    @Test
    void createRejectsDuplicateUid() {
        when(projectService.getById(projectId)).thenReturn(project);
        when(repository.existsByProjectIdAndUid(projectId, "TP-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateTreatmentPlanCommand(
                        projectId,
                        "TP-1",
                        "Mitigate gateway",
                        recordId,
                        null,
                        TreatmentStrategy.MITIGATE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listByRiskRegisterRecordRequiresRecordInProject() {
        when(riskRegisterRecordRepository.findByIdAndProjectIdWithScenarios(recordId, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByRiskRegisterRecord(projectId, recordId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsScenarioOutsideLinkedRecord() {
        var otherScenario = new RiskScenario(project, "RS-2", "Other", "Actor", "Exploit", "App", "Outage");
        otherScenario.setTimeHorizon("12 months");
        var otherScenarioId = UUID.randomUUID();
        setField(otherScenario, "id", otherScenarioId);
        var plan = new TreatmentPlan(project, "TP-1", "Mitigate gateway", record, TreatmentStrategy.MITIGATE);
        var planId = UUID.randomUUID();
        setField(plan, "id", planId);
        when(repository.findByIdAndProjectId(planId, projectId)).thenReturn(Optional.of(plan));
        when(riskScenarioRepository.findByIdAndProjectId(otherScenarioId, projectId))
                .thenReturn(Optional.of(otherScenario));

        assertThatThrownBy(() -> service.update(
                        projectId,
                        planId,
                        new UpdateTreatmentPlanCommand(
                                "Updated title",
                                otherScenarioId,
                                TreatmentStrategy.AVOID,
                                "Owner",
                                "Rationale",
                                Instant.parse("2026-06-01T00:00:00Z"),
                                List.of(),
                                List.of())))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("must belong");
    }

    @Test
    void transitionStatusUsesPlanStateMachine() {
        var plan = new TreatmentPlan(project, "TP-1", "Mitigate gateway", record, TreatmentStrategy.MITIGATE);
        var planId = UUID.randomUUID();
        setField(plan, "id", planId);
        when(repository.findByIdAndProjectId(planId, projectId)).thenReturn(Optional.of(plan));
        when(repository.save(plan)).thenReturn(plan);

        var transitioned = service.transitionStatus(projectId, planId, TreatmentPlanStatus.IN_PROGRESS);

        assertThat(transitioned.getStatus()).isEqualTo(TreatmentPlanStatus.IN_PROGRESS);
    }

    @Test
    void deleteRemovesResolvedPlan() {
        var plan = new TreatmentPlan(project, "TP-1", "Mitigate gateway", record, TreatmentStrategy.MITIGATE);
        var planId = UUID.randomUUID();
        setField(plan, "id", planId);
        when(repository.findByIdAndProjectId(planId, projectId)).thenReturn(Optional.of(plan));

        service.delete(projectId, planId);

        verify(repository).delete(plan);
    }
}
