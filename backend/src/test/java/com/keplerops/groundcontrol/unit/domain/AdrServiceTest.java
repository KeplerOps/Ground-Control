package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.adrs.model.ArchitectureDecisionRecord;
import com.keplerops.groundcontrol.domain.adrs.repository.AdrRepository;
import com.keplerops.groundcontrol.domain.adrs.service.AdrService;
import com.keplerops.groundcontrol.domain.adrs.service.CreateAdrCommand;
import com.keplerops.groundcontrol.domain.adrs.service.UpdateAdrCommand;
import com.keplerops.groundcontrol.domain.adrs.state.AdrStatus;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.model.Requirement;
import com.keplerops.groundcontrol.domain.requirements.model.TraceabilityLink;
import com.keplerops.groundcontrol.domain.requirements.repository.TraceabilityLinkRepository;
import com.keplerops.groundcontrol.domain.requirements.state.ArtifactType;
import com.keplerops.groundcontrol.domain.requirements.state.LinkType;
import java.time.LocalDate;
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
class AdrServiceTest {

    @Mock
    private AdrRepository adrRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private TraceabilityLinkRepository traceabilityLinkRepository;

    @InjectMocks
    private AdrService adrService;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private ArchitectureDecisionRecord createAdr(String uid, String title) {
        var adr = new ArchitectureDecisionRecord(
                project, uid, title, LocalDate.of(2026, 3, 31), "context", "decision", "consequences", "test-user");
        setField(adr, "id", UUID.randomUUID());
        return adr;
    }

    @Nested
    class Create {

        @Test
        void createSucceeds() {
            var command = new CreateAdrCommand(
                    projectId, "ADR-001", "Use PostgreSQL", LocalDate.of(2026, 3, 31), "ctx", "dec", "cons");
            when(projectService.getById(projectId)).thenReturn(project);
            when(adrRepository.existsByProjectIdAndUid(projectId, "ADR-001")).thenReturn(false);
            when(adrRepository.save(any())).thenAnswer(inv -> {
                var saved = inv.getArgument(0, ArchitectureDecisionRecord.class);
                setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            var result = adrService.create(command);

            assertThat(result.getUid()).isEqualTo("ADR-001");
            assertThat(result.getTitle()).isEqualTo("Use PostgreSQL");
            assertThat(result.getStatus()).isEqualTo(AdrStatus.PROPOSED);
        }

        @Test
        void createDuplicateUidThrowsConflict() {
            var command = new CreateAdrCommand(
                    projectId, "ADR-001", "Use PostgreSQL", LocalDate.of(2026, 3, 31), "ctx", "dec", "cons");
            when(projectService.getById(projectId)).thenReturn(project);
            when(adrRepository.existsByProjectIdAndUid(projectId, "ADR-001")).thenReturn(true);

            assertThatThrownBy(() -> adrService.create(command)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updateTitleOnly() {
            var adr = createAdr("ADR-001", "Old Title");
            when(adrRepository.findById(adr.getId())).thenReturn(Optional.of(adr));
            when(adrRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateAdrCommand("New Title", null, null, null, null, null);
            var result = adrService.update(adr.getId(), command);

            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getContext()).isEqualTo("context");
        }

        @Test
        void updateAllFields() {
            var adr = createAdr("ADR-001", "Old");
            when(adrRepository.findById(adr.getId())).thenReturn(Optional.of(adr));
            when(adrRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var command =
                    new UpdateAdrCommand("New", LocalDate.of(2026, 4, 1), "new ctx", "new dec", "new cons", "ADR-002");
            var result = adrService.update(adr.getId(), command);

            assertThat(result.getTitle()).isEqualTo("New");
            assertThat(result.getDecisionDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(result.getContext()).isEqualTo("new ctx");
            assertThat(result.getDecision()).isEqualTo("new dec");
            assertThat(result.getConsequences()).isEqualTo("new cons");
            assertThat(result.getSupersededBy()).isEqualTo("ADR-002");
        }
    }

    @Nested
    class Read {

        @Test
        void getByIdReturnsAdr() {
            var adr = createAdr("ADR-001", "Test");
            when(adrRepository.findById(adr.getId())).thenReturn(Optional.of(adr));

            var result = adrService.getById(adr.getId());
            assertThat(result.getUid()).isEqualTo("ADR-001");
        }

        @Test
        void getByIdNotFoundThrows() {
            var id = UUID.randomUUID();
            when(adrRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adrService.getById(id)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void getByUidReturnsAdr() {
            var adr = createAdr("ADR-001", "Test");
            when(adrRepository.findByProjectIdAndUid(projectId, "ADR-001")).thenReturn(Optional.of(adr));

            var result = adrService.getByUid("ADR-001", projectId);
            assertThat(result.getUid()).isEqualTo("ADR-001");
        }

        @Test
        void getByUidNotFoundThrows() {
            when(adrRepository.findByProjectIdAndUid(projectId, "ADR-999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adrService.getByUid("ADR-999", projectId)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void listByProjectReturnsList() {
            var adr1 = createAdr("ADR-001", "First");
            var adr2 = createAdr("ADR-002", "Second");
            when(adrRepository.findByProjectIdOrderByDecisionDateDesc(projectId))
                    .thenReturn(List.of(adr1, adr2));

            var result = adrService.listByProject(projectId);
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class StatusTransition {

        @Test
        void transitionSucceeds() {
            var adr = createAdr("ADR-001", "Test");
            when(adrRepository.findById(adr.getId())).thenReturn(Optional.of(adr));
            when(adrRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = adrService.transitionStatus(adr.getId(), AdrStatus.ACCEPTED);
            assertThat(result.getStatus()).isEqualTo(AdrStatus.ACCEPTED);
        }

        @Test
        void invalidTransitionThrows() {
            var adr = createAdr("ADR-001", "Test");
            when(adrRepository.findById(adr.getId())).thenReturn(Optional.of(adr));

            assertThatThrownBy(() -> adrService.transitionStatus(adr.getId(), AdrStatus.DEPRECATED))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void deleteSucceeds() {
            var adr = createAdr("ADR-001", "Test");
            when(adrRepository.findById(adr.getId())).thenReturn(Optional.of(adr));

            adrService.delete(adr.getId());
            verify(adrRepository).delete(adr);
        }
    }

    @Nested
    class ReverseTraceability {

        @Test
        void findLinkedRequirementsReturnsRequirements() {
            var adr = createAdr("ADR-001", "Test");
            when(adrRepository.findById(adr.getId())).thenReturn(Optional.of(adr));

            var requirement = new Requirement(project, "GC-R001", "Test Req", "A test requirement");
            var link = new TraceabilityLink(requirement, ArtifactType.ADR, "ADR-001", LinkType.DOCUMENTS);
            when(traceabilityLinkRepository.findByArtifactTypeAndArtifactIdentifierWithRequirement(
                            ArtifactType.ADR, "ADR-001"))
                    .thenReturn(List.of(link));

            var result = adrService.findLinkedRequirements(adr.getId());
            assertThat(result).hasSize(1);
        }

        @Test
        void findLinkedRequirementsReturnsEmptyWhenNoLinks() {
            var adr = createAdr("ADR-001", "Test");
            when(adrRepository.findById(adr.getId())).thenReturn(Optional.of(adr));
            when(traceabilityLinkRepository.findByArtifactTypeAndArtifactIdentifierWithRequirement(
                            ArtifactType.ADR, "ADR-001"))
                    .thenReturn(List.of());

            var result = adrService.findLinkedRequirements(adr.getId());
            assertThat(result).isEmpty();
        }
    }
}
