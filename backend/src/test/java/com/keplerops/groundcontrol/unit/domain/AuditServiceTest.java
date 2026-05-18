package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.assets.repository.AssetLinkRepository;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.audits.model.Audit;
import com.keplerops.groundcontrol.domain.audits.repository.AuditLinkRepository;
import com.keplerops.groundcontrol.domain.audits.repository.AuditRepository;
import com.keplerops.groundcontrol.domain.audits.service.AuditService;
import com.keplerops.groundcontrol.domain.audits.service.CreateAuditCommand;
import com.keplerops.groundcontrol.domain.audits.service.UpdateAuditCommand;
import com.keplerops.groundcontrol.domain.audits.state.AuditStatus;
import com.keplerops.groundcontrol.domain.audits.state.AuditType;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository;
import com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioLinkRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
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
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private AuditLinkRepository auditLinkRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private AssetLinkRepository assetLinkRepository;

    @Mock
    private FindingLinkRepository findingLinkRepository;

    @Mock
    private RiskScenarioLinkRepository riskScenarioLinkRepository;

    @InjectMocks
    private AuditService auditService;

    private Project project;
    private UUID projectId;
    private static final Instant NOW = Instant.parse("2026-05-18T12:00:00Z");

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private Audit makeAudit() {
        var a = new Audit(
                project, "AUDIT-001", "Annual compliance audit", AuditType.INTERNAL, "All production systems.");
        setField(a, "id", UUID.randomUUID());
        setField(a, "createdAt", NOW);
        setField(a, "updatedAt", NOW);
        return a;
    }

    @Nested
    class Create {

        @Test
        void createsAuditWithAllFields() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(auditRepository.existsByProjectIdAndUid(projectId, "AUDIT-001"))
                    .thenReturn(false);
            when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new CreateAuditCommand(
                    projectId,
                    "AUDIT-001",
                    "Annual compliance audit",
                    AuditType.INTERNAL,
                    "All production systems.",
                    List.of("Assess controls"),
                    null,
                    List.of("alice"));

            var result = auditService.create(cmd);

            assertThat(result.getUid()).isEqualTo("AUDIT-001");
            assertThat(result.getTitle()).isEqualTo("Annual compliance audit");
            assertThat(result.getAuditType()).isEqualTo(AuditType.INTERNAL);
            assertThat(result.getScopeDescription()).isEqualTo("All production systems.");
            assertThat(result.getStatus()).isEqualTo(AuditStatus.PLANNED);
            assertThat(result.getObjectives()).containsExactly("Assess controls");
            assertThat(result.getTeamMembers()).containsExactly("alice");
        }

        @Test
        void rejectsPhaseWithNullKind() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(auditRepository.existsByProjectIdAndUid(projectId, "AUDIT-001"))
                    .thenReturn(false);

            var badPhases = List.of(new com.keplerops.groundcontrol.domain.audits.model.AuditPhase(
                    null, java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 31), null, null));
            var cmd = new CreateAuditCommand(
                    projectId, "AUDIT-001", "Annual", AuditType.INTERNAL, "Scope.", null, badPhases, null);

            assertThatThrownBy(() -> auditService.create(cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("kind");
        }

        @Test
        void rejectsPhaseWithInvertedPlannedDateRange() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(auditRepository.existsByProjectIdAndUid(projectId, "AUDIT-001"))
                    .thenReturn(false);

            var badPhases = List.of(new com.keplerops.groundcontrol.domain.audits.model.AuditPhase(
                    com.keplerops.groundcontrol.domain.audits.state.AuditPhaseKind.PLANNING,
                    java.time.LocalDate.of(2026, 2, 1),
                    java.time.LocalDate.of(2026, 1, 1),
                    null,
                    null));
            var cmd = new CreateAuditCommand(
                    projectId, "AUDIT-001", "Annual", AuditType.INTERNAL, "Scope.", null, badPhases, null);

            assertThatThrownBy(() -> auditService.create(cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("plannedEnd before plannedStart");
        }

        @Test
        void rejectsPhaseWithInvertedActualDateRange() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(auditRepository.existsByProjectIdAndUid(projectId, "AUDIT-001"))
                    .thenReturn(false);

            var badPhases = List.of(new com.keplerops.groundcontrol.domain.audits.model.AuditPhase(
                    com.keplerops.groundcontrol.domain.audits.state.AuditPhaseKind.FIELDWORK,
                    null,
                    null,
                    java.time.LocalDate.of(2026, 3, 15),
                    java.time.LocalDate.of(2026, 3, 1)));
            var cmd = new CreateAuditCommand(
                    projectId, "AUDIT-001", "Annual", AuditType.INTERNAL, "Scope.", null, badPhases, null);

            assertThatThrownBy(() -> auditService.create(cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("actualEnd before actualStart");
        }

        @Test
        void throwsOnDuplicateUid() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(auditRepository.existsByProjectIdAndUid(projectId, "AUDIT-001"))
                    .thenReturn(true);

            var cmd = new CreateAuditCommand(
                    projectId, "AUDIT-001", "Title", AuditType.INTERNAL, "Scope.", null, null, null);

            assertThatThrownBy(() -> auditService.create(cmd)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesAudit() {
            var a = makeAudit();
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));
            when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpdateAuditCommand("Updated title", null, null, null, null, null, false, false, false);
            var result = auditService.update(projectId, a.getId(), cmd);

            assertThat(result.getTitle()).isEqualTo("Updated title");
            assertThat(result.getAuditType()).isEqualTo(AuditType.INTERNAL);
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(auditRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            var cmd = new UpdateAuditCommand("x", null, null, null, null, null, false, false, false);

            assertThatThrownBy(() -> auditService.update(projectId, id, cmd)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void rejectsBlankTitle() {
            var a = makeAudit();
            var aId = a.getId();
            when(auditRepository.findByIdAndProjectId(aId, projectId)).thenReturn(Optional.of(a));

            var cmd = new UpdateAuditCommand("   ", null, null, null, null, null, false, false, false);

            assertThatThrownBy(() -> auditService.update(projectId, aId, cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("title");
        }

        @Test
        void clearsObjectivesWhenFlagSet() {
            var a = makeAudit();
            a.setObjectives(List.of("obj1"));
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));
            when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpdateAuditCommand(null, null, null, null, null, null, true, false, false);
            var result = auditService.update(projectId, a.getId(), cmd);

            assertThat(result.getObjectives()).isEmpty();
        }

        @Test
        void clearsPhasesWhenFlagSet() {
            var a = makeAudit();
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));
            when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpdateAuditCommand(null, null, null, null, null, null, false, true, false);
            var result = auditService.update(projectId, a.getId(), cmd);

            assertThat(result.getPhases()).isEmpty();
        }

        @Test
        void clearsTeamMembersWhenFlagSet() {
            var a = makeAudit();
            a.setTeamMembers(List.of("alice"));
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));
            when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpdateAuditCommand(null, null, null, null, null, null, false, false, true);
            var result = auditService.update(projectId, a.getId(), cmd);

            assertThat(result.getTeamMembers()).isEmpty();
        }
    }

    @Nested
    class TransitionStatus {

        @Test
        void transitionsPlannedToInProgress() {
            var a = makeAudit();
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));
            when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = auditService.transitionStatus(projectId, a.getId(), AuditStatus.IN_PROGRESS);

            assertThat(result.getStatus()).isEqualTo(AuditStatus.IN_PROGRESS);
        }

        @Test
        void throwsOnInvalidTransition() {
            var a = makeAudit();
            var aId = a.getId();
            when(auditRepository.findByIdAndProjectId(aId, projectId)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> auditService.transitionStatus(projectId, aId, AuditStatus.CLOSED))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsAudit() {
            var a = makeAudit();
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));

            var result = auditService.getById(projectId, a.getId());

            assertThat(result.getUid()).isEqualTo("AUDIT-001");
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(auditRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> auditService.getById(projectId, id)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class GetByUid {

        @Test
        void returnsAudit() {
            var a = makeAudit();
            when(auditRepository.findByProjectIdAndUid(projectId, "AUDIT-001")).thenReturn(Optional.of(a));

            var result = auditService.getByUid("AUDIT-001", projectId);

            assertThat(result.getId()).isEqualTo(a.getId());
        }

        @Test
        void throwsWhenNotFound() {
            when(auditRepository.findByProjectIdAndUid(projectId, "AUDIT-999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> auditService.getByUid("AUDIT-999", projectId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ListByProject {

        @Test
        void listsAudits() {
            when(auditRepository.findByProjectIdOrderByCreatedAtDesc(projectId)).thenReturn(List.of(makeAudit()));

            var result = auditService.listByProject(projectId);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesAuditWhenNoReverseLinks() {
            var a = makeAudit();
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));
            when(assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.AUDIT, a.getId(), projectId))
                    .thenReturn(List.of());
            when(findingLinkRepository.findFindingUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            FindingLinkTargetType.AUDIT, a.getId(), projectId))
                    .thenReturn(List.of());
            when(riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            RiskScenarioLinkTargetType.AUDIT_RECORD, a.getId(), projectId))
                    .thenReturn(List.of());
            when(auditLinkRepository.findByAuditId(a.getId())).thenReturn(List.of());

            auditService.delete(projectId, a.getId());

            verify(auditRepository).delete(a);
        }

        @Test
        void deletesOutboundLinksBeforeParent() {
            var a = makeAudit();
            var outboundLink = new com.keplerops.groundcontrol.domain.audits.model.AuditLink(
                    a,
                    com.keplerops.groundcontrol.domain.audits.state.AuditLinkTargetType.CONTROL,
                    UUID.randomUUID(),
                    null,
                    com.keplerops.groundcontrol.domain.audits.state.AuditLinkType.ASSESSES);
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));
            when(assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.AUDIT, a.getId(), projectId))
                    .thenReturn(List.of());
            when(findingLinkRepository.findFindingUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            FindingLinkTargetType.AUDIT, a.getId(), projectId))
                    .thenReturn(List.of());
            when(riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            RiskScenarioLinkTargetType.AUDIT_RECORD, a.getId(), projectId))
                    .thenReturn(List.of());
            when(auditLinkRepository.findByAuditId(a.getId())).thenReturn(List.of(outboundLink));

            auditService.delete(projectId, a.getId());

            var inOrder = org.mockito.Mockito.inOrder(auditLinkRepository, auditRepository);
            inOrder.verify(auditLinkRepository).deleteAll(List.of(outboundLink));
            inOrder.verify(auditRepository).delete(a);
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(auditRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> auditService.delete(projectId, id)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void rejectsDeleteWhenAssetLinkReferencesAudit() {
            var a = makeAudit();
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));
            when(assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.AUDIT, a.getId(), projectId))
                    .thenReturn(List.of("ASSET-A"));
            when(findingLinkRepository.findFindingUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            FindingLinkTargetType.AUDIT, a.getId(), projectId))
                    .thenReturn(List.of());
            when(riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            RiskScenarioLinkTargetType.AUDIT_RECORD, a.getId(), projectId))
                    .thenReturn(List.of());

            var aId = a.getId();
            var thrown = catchThrowableOfType(ConflictException.class, () -> auditService.delete(projectId, aId));
            assertThat(thrown)
                    .isNotNull()
                    .hasMessageContaining("reverse links")
                    .extracting("errorCode")
                    .isEqualTo("audit_referenced");
            assertThat(thrown.getDetail())
                    .containsEntry("auditUid", a.getUid())
                    .containsEntry("assetCount", 1)
                    .containsEntry("assetUids", (java.io.Serializable) List.of("ASSET-A"));
        }

        @Test
        void rejectsDeleteWhenFindingLinkReferencesAudit() {
            var a = makeAudit();
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));
            when(assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.AUDIT, a.getId(), projectId))
                    .thenReturn(List.of());
            when(findingLinkRepository.findFindingUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            FindingLinkTargetType.AUDIT, a.getId(), projectId))
                    .thenReturn(List.of("FIND-1"));
            when(riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            RiskScenarioLinkTargetType.AUDIT_RECORD, a.getId(), projectId))
                    .thenReturn(List.of());

            var aId = a.getId();
            var thrown = catchThrowableOfType(ConflictException.class, () -> auditService.delete(projectId, aId));
            assertThat(thrown).isNotNull().extracting("errorCode").isEqualTo("audit_referenced");
            assertThat(thrown.getDetail())
                    .containsEntry("findingCount", 1)
                    .containsEntry("findingUids", (java.io.Serializable) List.of("FIND-1"));
        }

        @Test
        void rejectsDeleteWhenRiskScenarioLinkReferencesAudit() {
            var a = makeAudit();
            when(auditRepository.findByIdAndProjectId(a.getId(), projectId)).thenReturn(Optional.of(a));
            when(assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.AUDIT, a.getId(), projectId))
                    .thenReturn(List.of());
            when(findingLinkRepository.findFindingUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            FindingLinkTargetType.AUDIT, a.getId(), projectId))
                    .thenReturn(List.of());
            when(riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            RiskScenarioLinkTargetType.AUDIT_RECORD, a.getId(), projectId))
                    .thenReturn(List.of("RS-001"));

            var aId = a.getId();
            var thrown = catchThrowableOfType(ConflictException.class, () -> auditService.delete(projectId, aId));
            assertThat(thrown).isNotNull().extracting("errorCode").isEqualTo("audit_referenced");
            assertThat(thrown.getDetail())
                    .containsEntry("scenarioCount", 1)
                    .containsEntry("scenarioUids", (java.io.Serializable) List.of("RS-001"));
        }
    }
}
