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
import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModel;
import com.keplerops.groundcontrol.domain.threatmodels.model.ThreatModelLink;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelLinkRepository;
import com.keplerops.groundcontrol.domain.threatmodels.repository.ThreatModelRepository;
import com.keplerops.groundcontrol.domain.threatmodels.service.CreateThreatModelLinkCommand;
import com.keplerops.groundcontrol.domain.threatmodels.service.ThreatModelLinkService;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkTargetType;
import com.keplerops.groundcontrol.domain.threatmodels.state.ThreatModelLinkType;
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
class ThreatModelLinkServiceTest {

    @Mock
    private ThreatModelLinkRepository linkRepository;

    @Mock
    private ThreatModelRepository threatModelRepository;

    @Mock
    private GraphTargetResolverService graphTargetResolverService;

    @InjectMocks
    private ThreatModelLinkService linkService;

    private ThreatModel threatModel;
    private UUID threatModelId;
    private UUID projectId;
    private static final Instant NOW = Instant.parse("2026-04-11T12:00:00Z");

    @BeforeEach
    void setUp() {
        var project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
        threatModel = new ThreatModel(
                project, "TM-001", "Credential stuffing", "External actor", "Credential replay", "Account takeover");
        threatModelId = UUID.randomUUID();
        setField(threatModel, "id", threatModelId);
    }

    private ThreatModelLink makeLink() {
        var link = new ThreatModelLink(
                threatModel,
                ThreatModelLinkTargetType.CONTROL,
                UUID.randomUUID(),
                null,
                ThreatModelLinkType.MITIGATED_BY);
        link.setTargetTitle("MFA Policy");
        setField(link, "id", UUID.randomUUID());
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    @Nested
    class Create {

        @Test
        void createsInternalLink() {
            var assetId = UUID.randomUUID();
            when(threatModelRepository.findByIdAndProjectId(threatModelId, projectId))
                    .thenReturn(Optional.of(threatModel));
            when(graphTargetResolverService.validateThreatModelTarget(
                            projectId, ThreatModelLinkTargetType.ASSET, assetId, null))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(assetId, null, true));
            when(linkRepository.existsByThreatModelIdAndTargetTypeAndTargetEntityIdAndLinkType(
                            any(), any(), any(), any()))
                    .thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateThreatModelLinkCommand(
                    ThreatModelLinkTargetType.ASSET,
                    assetId,
                    null,
                    ThreatModelLinkType.AFFECTS,
                    null,
                    "Customer portal");

            var result = linkService.create(projectId, threatModelId, command);

            assertThat(result.getTargetType()).isEqualTo(ThreatModelLinkTargetType.ASSET);
            assertThat(result.getTargetEntityId()).isEqualTo(assetId);
            assertThat(result.getTargetIdentifier()).isNull();
            assertThat(result.getLinkType()).isEqualTo(ThreatModelLinkType.AFFECTS);
            assertThat(result.getTargetTitle()).isEqualTo("Customer portal");
        }

        @Test
        void createsExternalLink() {
            when(threatModelRepository.findByIdAndProjectId(threatModelId, projectId))
                    .thenReturn(Optional.of(threatModel));
            when(graphTargetResolverService.validateThreatModelTarget(
                            projectId, ThreatModelLinkTargetType.CODE, null, "backend/src/main/java/Auth.java"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(
                            null, "backend/src/main/java/Auth.java", false));
            when(linkRepository.existsByThreatModelIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            any(), any(), any(), any()))
                    .thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateThreatModelLinkCommand(
                    ThreatModelLinkTargetType.CODE,
                    null,
                    "backend/src/main/java/Auth.java",
                    ThreatModelLinkType.DOCUMENTED_IN,
                    "https://github.com/KeplerOps/Ground-Control/blob/main/backend/src/main/java/Auth.java",
                    "Auth.java");

            var result = linkService.create(projectId, threatModelId, command);

            assertThat(result.getTargetType()).isEqualTo(ThreatModelLinkTargetType.CODE);
            assertThat(result.getTargetIdentifier()).isEqualTo("backend/src/main/java/Auth.java");
            assertThat(result.getTargetEntityId()).isNull();
            assertThat(result.getLinkType()).isEqualTo(ThreatModelLinkType.DOCUMENTED_IN);
        }

        @Test
        void throwsWhenThreatModelNotFound() {
            when(threatModelRepository.findByIdAndProjectId(threatModelId, projectId))
                    .thenReturn(Optional.empty());

            var command = new CreateThreatModelLinkCommand(
                    ThreatModelLinkTargetType.CONTROL,
                    UUID.randomUUID(),
                    null,
                    ThreatModelLinkType.MITIGATED_BY,
                    null,
                    null);

            assertThatThrownBy(() -> linkService.create(projectId, threatModelId, command))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsOnDuplicateInternalLink() {
            var controlId = UUID.randomUUID();
            when(threatModelRepository.findByIdAndProjectId(threatModelId, projectId))
                    .thenReturn(Optional.of(threatModel));
            when(graphTargetResolverService.validateThreatModelTarget(
                            projectId, ThreatModelLinkTargetType.CONTROL, controlId, null))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(controlId, null, true));
            when(linkRepository.existsByThreatModelIdAndTargetTypeAndTargetEntityIdAndLinkType(
                            threatModelId,
                            ThreatModelLinkTargetType.CONTROL,
                            controlId,
                            ThreatModelLinkType.MITIGATED_BY))
                    .thenReturn(true);

            var command = new CreateThreatModelLinkCommand(
                    ThreatModelLinkTargetType.CONTROL, controlId, null, ThreatModelLinkType.MITIGATED_BY, null, null);

            assertThatThrownBy(() -> linkService.create(projectId, threatModelId, command))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void throwsOnDuplicateExternalLink() {
            when(threatModelRepository.findByIdAndProjectId(threatModelId, projectId))
                    .thenReturn(Optional.of(threatModel));
            when(graphTargetResolverService.validateThreatModelTarget(
                            projectId, ThreatModelLinkTargetType.CODE, null, "backend/Auth.java"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "backend/Auth.java", false));
            when(linkRepository.existsByThreatModelIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            threatModelId,
                            ThreatModelLinkTargetType.CODE,
                            "backend/Auth.java",
                            ThreatModelLinkType.DOCUMENTED_IN))
                    .thenReturn(true);

            var command = new CreateThreatModelLinkCommand(
                    ThreatModelLinkTargetType.CODE,
                    null,
                    "backend/Auth.java",
                    ThreatModelLinkType.DOCUMENTED_IN,
                    null,
                    null);

            assertThatThrownBy(() -> linkService.create(projectId, threatModelId, command))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class List_ {

        @Test
        void listsAllLinks() {
            when(threatModelRepository.existsByIdAndProjectId(threatModelId, projectId))
                    .thenReturn(true);
            when(linkRepository.findByThreatModelId(threatModelId)).thenReturn(List.of(makeLink()));

            var result = linkService.listByThreatModel(projectId, threatModelId);

            assertThat(result).hasSize(1);
        }

        @Test
        void throwsWhenThreatModelNotFound() {
            when(threatModelRepository.existsByIdAndProjectId(threatModelId, projectId))
                    .thenReturn(false);

            assertThatThrownBy(() -> linkService.listByThreatModel(projectId, threatModelId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesLink() {
            var link = makeLink();
            when(linkRepository.findByIdAndThreatModelProjectId(link.getId(), projectId))
                    .thenReturn(Optional.of(link));

            linkService.delete(projectId, threatModelId, link.getId());

            verify(linkRepository).delete(link);
        }

        @Test
        void throwsWhenLinkNotFound() {
            var linkId = UUID.randomUUID();
            when(linkRepository.findByIdAndThreatModelProjectId(linkId, projectId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> linkService.delete(projectId, threatModelId, linkId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsWhenLinkBelongsToDifferentThreatModel() {
            var link = makeLink();
            when(linkRepository.findByIdAndThreatModelProjectId(link.getId(), projectId))
                    .thenReturn(Optional.of(link));

            var wrongThreatModelId = UUID.randomUUID();
            assertThatThrownBy(() -> linkService.delete(projectId, wrongThreatModelId, link.getId()))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
