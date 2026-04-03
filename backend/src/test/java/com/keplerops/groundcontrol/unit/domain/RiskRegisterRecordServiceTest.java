package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskRegisterRecord;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskRegisterRecordRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.service.CreateRiskRegisterRecordCommand;
import com.keplerops.groundcontrol.domain.riskscenarios.service.RiskRegisterRecordService;
import com.keplerops.groundcontrol.domain.riskscenarios.service.UpdateRiskRegisterRecordCommand;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskRegisterStatus;
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
class RiskRegisterRecordServiceTest {

    @Mock
    private RiskRegisterRecordRepository repository;

    @Mock
    private RiskScenarioRepository riskScenarioRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private RiskRegisterRecordService service;

    private Project project;
    private UUID projectId;
    private RiskScenario scenario;
    private UUID scenarioId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
        scenario = new RiskScenario(project, "RS-1", "Gateway risk", "Actor", "Exploit", "Gateway", "Outage");
        scenario.setTimeHorizon("12 months");
        scenarioId = UUID.randomUUID();
        setField(scenario, "id", scenarioId);
    }

    @Test
    void createBuildsRecordWithResolvedScenarioLinks() {
        when(projectService.getById(projectId)).thenReturn(project);
        when(repository.existsByProjectIdAndUid(projectId, "RR-1")).thenReturn(false);
        when(riskScenarioRepository.findByIdInAndProjectId(List.of(scenarioId), projectId))
                .thenReturn(List.of(scenario));
        when(repository.save(any(RiskRegisterRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(new CreateRiskRegisterRecordCommand(
                projectId,
                "RR-1",
                "Gateway record",
                "Owner",
                "Quarterly",
                Instant.parse("2026-07-01T00:00:00Z"),
                List.of("availability"),
                Map.of("decision", "track"),
                "Gateway estate",
                List.of(scenarioId)));

        assertThat(result.getUid()).isEqualTo("RR-1");
        assertThat(result.getOwner()).isEqualTo("Owner");
        assertThat(result.getRiskScenarios()).containsExactly(scenario);
        assertThat(result.getDecisionMetadata()).containsEntry("decision", "track");
    }

    @Test
    void createRejectsDuplicateUid() {
        when(projectService.getById(projectId)).thenReturn(project);
        when(repository.existsByProjectIdAndUid(projectId, "RR-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateRiskRegisterRecordCommand(
                        projectId, "RR-1", "Gateway record", null, null, null, null, null, null, List.of())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRejectsScenarioIdsOutsideProject() {
        when(projectService.getById(projectId)).thenReturn(project);
        when(repository.existsByProjectIdAndUid(projectId, "RR-1")).thenReturn(false);
        when(riskScenarioRepository.findByIdInAndProjectId(List.of(scenarioId), projectId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.create(new CreateRiskRegisterRecordCommand(
                        projectId, "RR-1", "Gateway record", null, null, null, null, null, null, List.of(scenarioId))))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("risk scenarios");
    }

    @Test
    void updateMutatesFieldsAndReplacesScenarios() {
        var record = new RiskRegisterRecord(project, "RR-1", "Gateway record");
        var recordId = UUID.randomUUID();
        setField(record, "id", recordId);
        when(repository.findByIdAndProjectIdWithScenarios(recordId, projectId)).thenReturn(Optional.of(record));
        when(riskScenarioRepository.findByIdInAndProjectId(List.of(scenarioId), projectId))
                .thenReturn(List.of(scenario));
        when(repository.save(record)).thenReturn(record);

        var updated = service.update(
                projectId,
                recordId,
                new UpdateRiskRegisterRecordCommand(
                        "Updated title",
                        "Owner",
                        "Monthly",
                        Instant.parse("2026-05-01T00:00:00Z"),
                        List.of("integrity"),
                        Map.of("decision", "mitigate"),
                        "Scope",
                        List.of(scenarioId)));

        assertThat(updated.getTitle()).isEqualTo("Updated title");
        assertThat(updated.getReviewCadence()).isEqualTo("Monthly");
        assertThat(updated.getRiskScenarios()).containsExactly(scenario);
        assertThat(updated.getAssetScopeSummary()).isEqualTo("Scope");
    }

    @Test
    void transitionStatusUsesDomainTransitionRules() {
        var record = new RiskRegisterRecord(project, "RR-1", "Gateway record");
        var recordId = UUID.randomUUID();
        setField(record, "id", recordId);
        when(repository.findByIdAndProjectIdWithScenarios(recordId, projectId)).thenReturn(Optional.of(record));
        when(repository.save(record)).thenReturn(record);

        var transitioned = service.transitionStatus(projectId, recordId, RiskRegisterStatus.ANALYZING);

        assertThat(transitioned.getStatus()).isEqualTo(RiskRegisterStatus.ANALYZING);
    }

    @Test
    void deleteRemovesResolvedRecord() {
        var record = new RiskRegisterRecord(project, "RR-1", "Gateway record");
        var recordId = UUID.randomUUID();
        setField(record, "id", recordId);
        when(repository.findByIdAndProjectIdWithScenarios(recordId, projectId)).thenReturn(Optional.of(record));

        service.delete(projectId, recordId);

        verify(repository).delete(record);
    }
}
