package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.audits.model.Audit;
import com.keplerops.groundcontrol.domain.audits.model.AuditLink;
import com.keplerops.groundcontrol.domain.audits.repository.AuditLinkRepository;
import com.keplerops.groundcontrol.domain.audits.repository.AuditRepository;
import com.keplerops.groundcontrol.domain.audits.service.AuditLinkService;
import com.keplerops.groundcontrol.domain.audits.service.CreateAuditLinkCommand;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkTargetType;
import com.keplerops.groundcontrol.domain.audits.state.AuditLinkType;
import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
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
class AuditLinkServiceTest {

    @Mock
    private AuditLinkRepository linkRepository;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private GraphTargetResolverService graphTargetResolverService;

    @InjectMocks
    private AuditLinkService linkService;

    private Audit audit;
    private UUID auditId;
    private UUID projectId;
    private static final Instant NOW = Instant.parse("2026-05-18T12:00:00Z");

    @BeforeEach
    void setUp() {
        var project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
        audit = new Audit(project, "AUDIT-001", "Annual compliance audit", AuditType.INTERNAL, "All prod systems.");
        auditId = UUID.randomUUID();
        setField(audit, "id", auditId);
    }

    private AuditLink makeLink() {
        var link = new AuditLink(audit, AuditLinkTargetType.CONTROL, UUID.randomUUID(), null, AuditLinkType.ASSESSES);
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
            when(auditRepository.findByIdAndProjectId(auditId, projectId)).thenReturn(Optional.of(audit));
            when(graphTargetResolverService.validateAuditTarget(
                            projectId, AuditLinkTargetType.CONTROL, controlId, null))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(controlId, null, true));
            when(linkRepository.existsByAuditIdAndTargetTypeAndTargetEntityIdAndLinkType(any(), any(), any(), any()))
                    .thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateAuditLinkCommand(
                    AuditLinkTargetType.CONTROL, controlId, null, AuditLinkType.ASSESSES, null, "Access policy");

            var result = linkService.create(projectId, auditId, command);

            assertThat(result.getTargetType()).isEqualTo(AuditLinkTargetType.CONTROL);
            assertThat(result.getTargetEntityId()).isEqualTo(controlId);
            assertThat(result.getTargetIdentifier()).isNull();
            assertThat(result.getLinkType()).isEqualTo(AuditLinkType.ASSESSES);
            assertThat(result.getTargetTitle()).isEqualTo("Access policy");
        }

        @Test
        void createsExternalLink() {
            when(auditRepository.findByIdAndProjectId(auditId, projectId)).thenReturn(Optional.of(audit));
            when(graphTargetResolverService.validateAuditTarget(
                            projectId, AuditLinkTargetType.FRAMEWORK, null, "ISO-27001"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "ISO-27001", false));
            when(linkRepository.existsByAuditIdAndTargetTypeAndTargetIdentifierAndLinkType(any(), any(), any(), any()))
                    .thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateAuditLinkCommand(
                    AuditLinkTargetType.FRAMEWORK,
                    null,
                    "ISO-27001",
                    AuditLinkType.SCOPES,
                    "https://iso.org/27001",
                    "ISO 27001:2022");

            var result = linkService.create(projectId, auditId, command);

            assertThat(result.getTargetType()).isEqualTo(AuditLinkTargetType.FRAMEWORK);
            assertThat(result.getTargetIdentifier()).isEqualTo("ISO-27001");
            assertThat(result.getTargetEntityId()).isNull();
            assertThat(result.getLinkType()).isEqualTo(AuditLinkType.SCOPES);
        }

        @Test
        void throwsWhenAuditNotFound() {
            when(auditRepository.findByIdAndProjectId(auditId, projectId)).thenReturn(Optional.empty());

            var command = new CreateAuditLinkCommand(
                    AuditLinkTargetType.CONTROL, UUID.randomUUID(), null, AuditLinkType.ASSESSES, null, null);

            assertThatThrownBy(() -> linkService.create(projectId, auditId, command))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsOnDuplicateInternalLink() {
            var controlId = UUID.randomUUID();
            when(auditRepository.findByIdAndProjectId(auditId, projectId)).thenReturn(Optional.of(audit));
            when(graphTargetResolverService.validateAuditTarget(
                            projectId, AuditLinkTargetType.CONTROL, controlId, null))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(controlId, null, true));
            when(linkRepository.existsByAuditIdAndTargetTypeAndTargetEntityIdAndLinkType(
                            auditId, AuditLinkTargetType.CONTROL, controlId, AuditLinkType.ASSESSES))
                    .thenReturn(true);

            var command = new CreateAuditLinkCommand(
                    AuditLinkTargetType.CONTROL, controlId, null, AuditLinkType.ASSESSES, null, null);

            assertThatThrownBy(() -> linkService.create(projectId, auditId, command))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void throwsOnDuplicateExternalLink() {
            when(auditRepository.findByIdAndProjectId(auditId, projectId)).thenReturn(Optional.of(audit));
            when(graphTargetResolverService.validateAuditTarget(
                            projectId, AuditLinkTargetType.FRAMEWORK, null, "ISO-27001"))
                    .thenReturn(new GraphTargetResolverService.ValidatedTarget(null, "ISO-27001", false));
            when(linkRepository.existsByAuditIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            auditId, AuditLinkTargetType.FRAMEWORK, "ISO-27001", AuditLinkType.SCOPES))
                    .thenReturn(true);

            var command = new CreateAuditLinkCommand(
                    AuditLinkTargetType.FRAMEWORK, null, "ISO-27001", AuditLinkType.SCOPES, null, null);

            assertThatThrownBy(() -> linkService.create(projectId, auditId, command))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class ListByAudit {

        @Test
        void listsAllLinks() {
            when(auditRepository.existsByIdAndProjectId(auditId, projectId)).thenReturn(true);
            when(linkRepository.findByAuditId(auditId)).thenReturn(List.of(makeLink()));

            var result = linkService.listByAudit(projectId, auditId);

            assertThat(result).hasSize(1);
        }

        @Test
        void throwsWhenAuditNotFound() {
            when(auditRepository.existsByIdAndProjectId(auditId, projectId)).thenReturn(false);

            assertThatThrownBy(() -> linkService.listByAudit(projectId, auditId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesLink() {
            var link = makeLink();
            when(linkRepository.findByIdAndAuditProjectId(link.getId(), projectId))
                    .thenReturn(Optional.of(link));

            linkService.delete(projectId, auditId, link.getId());

            verify(linkRepository).delete(link);
        }

        @Test
        void throwsWhenLinkNotFound() {
            var linkId = UUID.randomUUID();
            when(linkRepository.findByIdAndAuditProjectId(linkId, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> linkService.delete(projectId, auditId, linkId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void throwsWhenLinkBelongsToDifferentAudit() {
            var link = makeLink();
            var linkId = link.getId();
            when(linkRepository.findByIdAndAuditProjectId(linkId, projectId)).thenReturn(Optional.of(link));

            var wrongAuditId = UUID.randomUUID();
            assertThatThrownBy(() -> linkService.delete(projectId, wrongAuditId, linkId))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
