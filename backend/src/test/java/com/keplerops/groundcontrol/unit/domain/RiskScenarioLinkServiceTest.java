package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.graph.service.GraphTargetResolverService;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenario;
import com.keplerops.groundcontrol.domain.riskscenarios.model.RiskScenarioLink;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioLinkRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.service.RiskScenarioLinkService;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkType;
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
class RiskScenarioLinkServiceTest {

    @Mock
    private RiskScenarioLinkRepository linkRepository;

    @Mock
    private RiskScenarioRepository riskScenarioRepository;

    @Mock
    private GraphTargetResolverService graphTargetResolverService;

    @InjectMocks
    private RiskScenarioLinkService linkService;

    private RiskScenario scenario;
    private UUID scenarioId;
    private UUID projectId;
    private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        var project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
        scenario = new RiskScenario(project, "RS-001", "Test scenario", "Source", "Event", "Object", "Consequence");
        scenario.setTimeHorizon("12 months");
        scenarioId = UUID.randomUUID();
        setField(scenario, "id", scenarioId);
    }

    private RiskScenarioLink makeLink() {
        var link = new RiskScenarioLink(
                scenario, RiskScenarioLinkTargetType.CONTROL, null, "CTRL-001", RiskScenarioLinkType.MITIGATED_BY);
        link.setTargetTitle("MFA Policy");
        setField(link, "id", UUID.randomUUID());
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    @Nested
    class Create {

        @Test
        void createsLink() {
            when(riskScenarioRepository.findById(scenarioId)).thenReturn(Optional.of(scenario));
            when(graphTargetResolverService.validateRiskScenarioTarget(
                            projectId, RiskScenarioLinkTargetType.CONTROL, null, "CTRL-001"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "CTRL-001", false));
            when(linkRepository.existsByRiskScenarioIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            any(), any(), any(), any()))
                    .thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = linkService.create(
                    scenarioId,
                    RiskScenarioLinkTargetType.CONTROL,
                    null,
                    "CTRL-001",
                    RiskScenarioLinkType.MITIGATED_BY,
                    null,
                    "MFA Policy");

            assertThat(result.getTargetType()).isEqualTo(RiskScenarioLinkTargetType.CONTROL);
            assertThat(result.getTargetIdentifier()).isEqualTo("CTRL-001");
            assertThat(result.getLinkType()).isEqualTo(RiskScenarioLinkType.MITIGATED_BY);
            assertThat(result.getTargetTitle()).isEqualTo("MFA Policy");
        }

        @Test
        void throwsWhenScenarioNotFound() {
            when(riskScenarioRepository.findById(scenarioId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> linkService.create(
                            scenarioId,
                            RiskScenarioLinkTargetType.CONTROL,
                            null,
                            "CTRL-001",
                            RiskScenarioLinkType.MITIGATED_BY,
                            null,
                            null))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsOnDuplicate() {
            when(riskScenarioRepository.findById(scenarioId)).thenReturn(Optional.of(scenario));
            when(graphTargetResolverService.validateRiskScenarioTarget(
                            projectId, RiskScenarioLinkTargetType.CONTROL, null, "CTRL-001"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "CTRL-001", false));
            when(linkRepository.existsByRiskScenarioIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            scenarioId,
                            RiskScenarioLinkTargetType.CONTROL,
                            "CTRL-001",
                            RiskScenarioLinkType.MITIGATED_BY))
                    .thenReturn(true);

            assertThatThrownBy(() -> linkService.create(
                            scenarioId,
                            RiskScenarioLinkTargetType.CONTROL,
                            null,
                            "CTRL-001",
                            RiskScenarioLinkType.MITIGATED_BY,
                            null,
                            null))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class ListByScenario {

        @Test
        void listsAllLinks() {
            when(riskScenarioRepository.existsById(scenarioId)).thenReturn(true);
            when(linkRepository.findByRiskScenarioId(scenarioId)).thenReturn(List.of(makeLink()));

            var result = linkService.listByScenario(scenarioId, null);

            assertThat(result).hasSize(1);
        }

        @Test
        void filtersByTargetType() {
            when(riskScenarioRepository.existsById(scenarioId)).thenReturn(true);
            when(linkRepository.findByRiskScenarioIdAndTargetType(scenarioId, RiskScenarioLinkTargetType.CONTROL))
                    .thenReturn(List.of(makeLink()));

            var result = linkService.listByScenario(scenarioId, RiskScenarioLinkTargetType.CONTROL);

            assertThat(result).hasSize(1);
        }

        @Test
        void throwsWhenScenarioNotFound() {
            when(riskScenarioRepository.existsById(scenarioId)).thenReturn(false);

            assertThatThrownBy(() -> linkService.listByScenario(scenarioId, null))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesLink() {
            var link = makeLink();
            when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));

            linkService.delete(scenarioId, link.getId());

            verify(linkRepository).delete(link);
        }

        @Test
        void throwsWhenLinkNotFound() {
            var linkId = UUID.randomUUID();
            when(linkRepository.findById(linkId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> linkService.delete(scenarioId, linkId)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsWhenLinkBelongsToDifferentScenario() {
            var link = makeLink();
            var linkId = link.getId();
            var otherScenarioId = UUID.randomUUID();
            when(linkRepository.findById(linkId)).thenReturn(Optional.of(link));

            assertThatThrownBy(() -> linkService.delete(otherScenarioId, linkId))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
