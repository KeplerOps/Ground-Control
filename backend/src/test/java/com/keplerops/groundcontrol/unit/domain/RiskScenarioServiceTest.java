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
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.service.CreateRiskScenarioCommand;
import com.keplerops.groundcontrol.domain.riskscenarios.service.RiskScenarioService;
import com.keplerops.groundcontrol.domain.riskscenarios.service.UpdateRiskScenarioCommand;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiskScenarioServiceTest {

    @Mock
    private RiskScenarioRepository riskScenarioRepository;

    @Mock
    private ProjectService projectService;

    @SuppressWarnings("UnusedVariable") // needed by @InjectMocks for constructor injection
    @Mock
    private TraceabilityLinkRepository traceabilityLinkRepository;

    @InjectMocks
    private RiskScenarioService riskScenarioService;

    private Project project;
    private UUID projectId;
    private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private RiskScenario makeScenario() {
        var rs = new RiskScenario(
                project,
                "RS-001",
                "Credential stuffing on customer portal",
                "External threat actor",
                "Credential stuffing attack",
                "Customer authentication portal",
                "Data breach and unauthorized access",
                "12 months",
                "system");
        rs.setVulnerability("Weak password policy");
        rs.setObservationRefs("OBS-001, OBS-002");
        rs.setTopologyContext("DMZ web tier");
        setField(rs, "id", UUID.randomUUID());
        setField(rs, "createdAt", NOW);
        setField(rs, "updatedAt", NOW);
        return rs;
    }

    @Nested
    class Create {

        @Test
        void createsRiskScenario() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(riskScenarioRepository.existsByProjectIdAndUid(projectId, "RS-001"))
                    .thenReturn(false);
            when(riskScenarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateRiskScenarioCommand(
                    projectId,
                    "RS-001",
                    "Credential stuffing",
                    "External actor",
                    "Credential stuffing",
                    "Auth portal",
                    "Weak passwords",
                    "Data breach",
                    "12 months",
                    "OBS-001",
                    "DMZ");

            var result = riskScenarioService.create(command);

            assertThat(result.getUid()).isEqualTo("RS-001");
            assertThat(result.getTitle()).isEqualTo("Credential stuffing");
            assertThat(result.getThreatSource()).isEqualTo("External actor");
            assertThat(result.getVulnerability()).isEqualTo("Weak passwords");
            assertThat(result.getStatus()).isEqualTo(RiskScenarioStatus.DRAFT);
        }

        @Test
        void throwsOnDuplicateUid() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(riskScenarioRepository.existsByProjectIdAndUid(projectId, "RS-001"))
                    .thenReturn(true);

            var command = new CreateRiskScenarioCommand(
                    projectId, "RS-001", "Title", "Source", "Event", "Object", null, "Consequence", "12m", null, null);

            assertThatThrownBy(() -> riskScenarioService.create(command)).isInstanceOf(ConflictException.class);
        }

        @Test
        void createsWithNullOptionalFields() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(riskScenarioRepository.existsByProjectIdAndUid(any(), any())).thenReturn(false);
            when(riskScenarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateRiskScenarioCommand(
                    projectId, "RS-002", "Title", "Source", "Event", "Object", null, "Consequence", "6m", null, null);

            var result = riskScenarioService.create(command);

            assertThat(result.getVulnerability()).isNull();
            assertThat(result.getObservationRefs()).isNull();
            assertThat(result.getTopologyContext()).isNull();
        }
    }

    @Nested
    class Update {

        @Test
        void updatesRiskScenario() {
            var rs = makeScenario();
            when(riskScenarioRepository.findById(rs.getId())).thenReturn(Optional.of(rs));
            when(riskScenarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command =
                    new UpdateRiskScenarioCommand("Updated title", null, null, null, null, null, null, null, null);
            var result = riskScenarioService.update(rs.getId(), command);

            assertThat(result.getTitle()).isEqualTo("Updated title");
            assertThat(result.getThreatSource()).isEqualTo("External threat actor");
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(riskScenarioRepository.findById(id)).thenReturn(Optional.empty());

            var command = new UpdateRiskScenarioCommand("Title", null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> riskScenarioService.update(id, command)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class TransitionStatus {

        @Test
        void transitionsFromDraftToIdentified() {
            var rs = makeScenario();
            when(riskScenarioRepository.findById(rs.getId())).thenReturn(Optional.of(rs));
            when(riskScenarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = riskScenarioService.transitionStatus(rs.getId(), RiskScenarioStatus.IDENTIFIED);

            assertThat(result.getStatus()).isEqualTo(RiskScenarioStatus.IDENTIFIED);
        }

        @Test
        void throwsOnInvalidTransition() {
            var rs = makeScenario();
            when(riskScenarioRepository.findById(rs.getId())).thenReturn(Optional.of(rs));

            assertThatThrownBy(() -> riskScenarioService.transitionStatus(rs.getId(), RiskScenarioStatus.CLOSED))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsScenario() {
            var rs = makeScenario();
            when(riskScenarioRepository.findById(rs.getId())).thenReturn(Optional.of(rs));

            var result = riskScenarioService.getById(rs.getId());

            assertThat(result.getUid()).isEqualTo("RS-001");
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(riskScenarioRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> riskScenarioService.getById(id)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ListByProject {

        @Test
        void listsScenarios() {
            when(riskScenarioRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                    .thenReturn(List.of(makeScenario()));

            var result = riskScenarioService.listByProject(projectId);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesScenario() {
            var rs = makeScenario();
            when(riskScenarioRepository.findById(rs.getId())).thenReturn(Optional.of(rs));

            riskScenarioService.delete(rs.getId());

            verify(riskScenarioRepository).delete(rs);
        }
    }
}
