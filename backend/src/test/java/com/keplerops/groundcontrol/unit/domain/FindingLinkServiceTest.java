package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.findings.model.Finding;
import com.keplerops.groundcontrol.domain.findings.model.FindingLink;
import com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository;
import com.keplerops.groundcontrol.domain.findings.repository.FindingRepository;
import com.keplerops.groundcontrol.domain.findings.service.CreateFindingLinkCommand;
import com.keplerops.groundcontrol.domain.findings.service.FindingLinkService;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkType;
import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import com.keplerops.groundcontrol.domain.graph.service.GraphTargetResolverService;
import com.keplerops.groundcontrol.domain.projects.model.Project;
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
class FindingLinkServiceTest {

    @Mock
    private FindingLinkRepository linkRepository;

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private GraphTargetResolverService graphTargetResolverService;

    @InjectMocks
    private FindingLinkService linkService;

    private Finding finding;
    private UUID findingId;
    private UUID projectId;
    private static final Instant NOW = Instant.parse("2026-05-13T12:00:00Z");

    @BeforeEach
    void setUp() {
        var project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
        finding = new Finding(
                project,
                "FIND-001",
                "MFA missing on admin portal",
                FindingType.CONTROL_DEFICIENCY,
                FindingSeverity.HIGH,
                "Admin portal accepts password-only auth.");
        findingId = UUID.randomUUID();
        setField(finding, "id", findingId);
    }

    private FindingLink makeLink() {
        var link = new FindingLink(
                finding, FindingLinkTargetType.CONTROL, UUID.randomUUID(), null, FindingLinkType.MITIGATED_BY);
        setField(link, "id", UUID.randomUUID());
        setField(link, "createdAt", NOW);
        setField(link, "updatedAt", NOW);
        return link;
    }

    @Nested
    class Create {

        @Test
        void createsInternalLink() {
            var controlId = UUID.randomUUID();
            when(findingRepository.findByIdAndProjectId(findingId, projectId)).thenReturn(Optional.of(finding));
            when(graphTargetResolverService.validateFindingTarget(
                            projectId, FindingLinkTargetType.CONTROL, controlId, null))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(controlId, null, true));
            when(linkRepository.existsByFindingIdAndTargetTypeAndTargetEntityIdAndLinkType(any(), any(), any(), any()))
                    .thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateFindingLinkCommand(
                    FindingLinkTargetType.CONTROL,
                    controlId,
                    null,
                    FindingLinkType.MITIGATED_BY,
                    null,
                    "Access policy");

            var result = linkService.create(projectId, findingId, command);

            assertThat(result.getTargetType()).isEqualTo(FindingLinkTargetType.CONTROL);
            assertThat(result.getTargetEntityId()).isEqualTo(controlId);
            assertThat(result.getTargetIdentifier()).isNull();
            assertThat(result.getLinkType()).isEqualTo(FindingLinkType.MITIGATED_BY);
            assertThat(result.getTargetTitle()).isEqualTo("Access policy");
        }

        @Test
        void createsExternalLink() {
            when(findingRepository.findByIdAndProjectId(findingId, projectId)).thenReturn(Optional.of(finding));
            when(graphTargetResolverService.validateFindingTarget(
                            projectId, FindingLinkTargetType.EVIDENCE, null, "s3://evidence/audit-2026-q2.pdf"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(
                            null, "s3://evidence/audit-2026-q2.pdf", false));
            when(linkRepository.existsByFindingIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            any(), any(), any(), any()))
                    .thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateFindingLinkCommand(
                    FindingLinkTargetType.EVIDENCE,
                    null,
                    "s3://evidence/audit-2026-q2.pdf",
                    FindingLinkType.EVIDENCED_BY,
                    "https://evidence.example.com/audit-2026-q2",
                    "Q2 2026 audit report");

            var result = linkService.create(projectId, findingId, command);

            assertThat(result.getTargetType()).isEqualTo(FindingLinkTargetType.EVIDENCE);
            assertThat(result.getTargetIdentifier()).isEqualTo("s3://evidence/audit-2026-q2.pdf");
            assertThat(result.getTargetEntityId()).isNull();
            assertThat(result.getLinkType()).isEqualTo(FindingLinkType.EVIDENCED_BY);
        }

        @Test
        void throwsWhenFindingNotFound() {
            when(findingRepository.findByIdAndProjectId(findingId, projectId)).thenReturn(Optional.empty());

            var command = new CreateFindingLinkCommand(
                    FindingLinkTargetType.CONTROL, UUID.randomUUID(), null, FindingLinkType.MITIGATED_BY, null, null);

            assertThatThrownBy(() -> linkService.create(projectId, findingId, command))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsOnDuplicateInternalLink() {
            var controlId = UUID.randomUUID();
            when(findingRepository.findByIdAndProjectId(findingId, projectId)).thenReturn(Optional.of(finding));
            when(graphTargetResolverService.validateFindingTarget(
                            projectId, FindingLinkTargetType.CONTROL, controlId, null))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(controlId, null, true));
            when(linkRepository.existsByFindingIdAndTargetTypeAndTargetEntityIdAndLinkType(
                            findingId, FindingLinkTargetType.CONTROL, controlId, FindingLinkType.MITIGATED_BY))
                    .thenReturn(true);

            var command = new CreateFindingLinkCommand(
                    FindingLinkTargetType.CONTROL, controlId, null, FindingLinkType.MITIGATED_BY, null, null);

            assertThatThrownBy(() -> linkService.create(projectId, findingId, command))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void throwsOnDuplicateExternalLink() {
            when(findingRepository.findByIdAndProjectId(findingId, projectId)).thenReturn(Optional.of(finding));
            when(graphTargetResolverService.validateFindingTarget(
                            projectId, FindingLinkTargetType.AUDIT, null, "audit-2026-q2"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "audit-2026-q2", false));
            when(linkRepository.existsByFindingIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            findingId, FindingLinkTargetType.AUDIT, "audit-2026-q2", FindingLinkType.OBSERVED_IN))
                    .thenReturn(true);

            var command = new CreateFindingLinkCommand(
                    FindingLinkTargetType.AUDIT, null, "audit-2026-q2", FindingLinkType.OBSERVED_IN, null, null);

            assertThatThrownBy(() -> linkService.create(projectId, findingId, command))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class List_ {

        @Test
        void listsAllLinks() {
            when(findingRepository.existsByIdAndProjectId(findingId, projectId)).thenReturn(true);
            when(linkRepository.findByFindingId(findingId)).thenReturn(List.of(makeLink()));

            var result = linkService.listByFinding(projectId, findingId);

            assertThat(result).hasSize(1);
        }

        @Test
        void throwsWhenFindingNotFound() {
            when(findingRepository.existsByIdAndProjectId(findingId, projectId)).thenReturn(false);

            assertThatThrownBy(() -> linkService.listByFinding(projectId, findingId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesLink() {
            var link = makeLink();
            when(linkRepository.findByIdAndFindingProjectId(link.getId(), projectId))
                    .thenReturn(Optional.of(link));

            linkService.delete(projectId, findingId, link.getId());

            verify(linkRepository).delete(link);
        }

        @Test
        void throwsWhenLinkNotFound() {
            var linkId = UUID.randomUUID();
            when(linkRepository.findByIdAndFindingProjectId(linkId, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> linkService.delete(projectId, findingId, linkId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsWhenLinkBelongsToDifferentFinding() {
            var link = makeLink();
            when(linkRepository.findByIdAndFindingProjectId(link.getId(), projectId))
                    .thenReturn(Optional.of(link));

            var wrongFindingId = UUID.randomUUID();
            assertThatThrownBy(() -> linkService.delete(projectId, wrongFindingId, link.getId()))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
