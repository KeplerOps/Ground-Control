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
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.findings.repository.FindingRepository;
import com.keplerops.groundcontrol.domain.findings.service.CreateFindingCommand;
import com.keplerops.groundcontrol.domain.findings.service.FindingService;
import com.keplerops.groundcontrol.domain.findings.service.UpdateFindingCommand;
import com.keplerops.groundcontrol.domain.findings.state.FindingSeverity;
import com.keplerops.groundcontrol.domain.findings.state.FindingStatus;
import com.keplerops.groundcontrol.domain.findings.state.FindingType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.repository.RiskScenarioLinkRepository;
import com.keplerops.groundcontrol.domain.riskscenarios.state.RiskScenarioLinkTargetType;
import java.time.Instant;
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
class FindingServiceTest {

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private com.keplerops.groundcontrol.domain.findings.repository.FindingLinkRepository findingLinkRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private AssetLinkRepository assetLinkRepository;

    @Mock
    private ControlLinkRepository controlLinkRepository;

    @Mock
    private RiskScenarioLinkRepository riskScenarioLinkRepository;

    @InjectMocks
    private FindingService findingService;

    private Project project;
    private UUID projectId;
    private static final Instant NOW = Instant.parse("2026-05-13T12:00:00Z");
    private static final LocalDate DUE = LocalDate.of(2026, 6, 30);

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private com.keplerops.groundcontrol.domain.findings.model.Finding makeFinding() {
        var f = new com.keplerops.groundcontrol.domain.findings.model.Finding(
                project,
                "FIND-001",
                "MFA missing on admin portal",
                FindingType.CONTROL_DEFICIENCY,
                FindingSeverity.HIGH,
                "Admin portal accepts password-only auth.");
        f.setRootCauseAnalysis("Identity provider misconfigured during migration.");
        f.setOwner("alice");
        f.setDueDate(DUE);
        f.setCreatedBy("analyst");
        setField(f, "id", UUID.randomUUID());
        setField(f, "createdAt", NOW);
        setField(f, "updatedAt", NOW);
        return f;
    }

    @Nested
    class Create {

        @Test
        void createsFindingWithAllFields() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(findingRepository.existsByProjectIdAndUid(projectId, "FIND-001"))
                    .thenReturn(false);
            when(findingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new CreateFindingCommand(
                    projectId,
                    "FIND-001",
                    "MFA missing",
                    FindingType.CONTROL_DEFICIENCY,
                    FindingSeverity.HIGH,
                    "Admin portal accepts password-only auth.",
                    "Identity provider misconfigured.",
                    "alice",
                    DUE);

            var result = findingService.create(cmd);

            assertThat(result.getUid()).isEqualTo("FIND-001");
            assertThat(result.getTitle()).isEqualTo("MFA missing");
            assertThat(result.getFindingType()).isEqualTo(FindingType.CONTROL_DEFICIENCY);
            assertThat(result.getSeverity()).isEqualTo(FindingSeverity.HIGH);
            assertThat(result.getDescription()).isEqualTo("Admin portal accepts password-only auth.");
            assertThat(result.getRootCauseAnalysis()).isEqualTo("Identity provider misconfigured.");
            assertThat(result.getOwner()).isEqualTo("alice");
            assertThat(result.getDueDate()).isEqualTo(DUE);
            assertThat(result.getStatus()).isEqualTo(FindingStatus.OPEN);
        }

        @Test
        void createsWithNullOptionalFields() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(findingRepository.existsByProjectIdAndUid(any(), any())).thenReturn(false);
            when(findingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new CreateFindingCommand(
                    projectId,
                    "FIND-002",
                    "Title",
                    FindingType.AUDIT_FINDING,
                    FindingSeverity.LOW,
                    "Description",
                    null,
                    null,
                    null);

            var result = findingService.create(cmd);

            assertThat(result.getRootCauseAnalysis()).isNull();
            assertThat(result.getOwner()).isNull();
            assertThat(result.getDueDate()).isNull();
        }

        @Test
        void throwsOnDuplicateUid() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(findingRepository.existsByProjectIdAndUid(projectId, "FIND-001"))
                    .thenReturn(true);

            var cmd = new CreateFindingCommand(
                    projectId,
                    "FIND-001",
                    "Title",
                    FindingType.AUDIT_FINDING,
                    FindingSeverity.LOW,
                    "Description",
                    null,
                    null,
                    null);

            assertThatThrownBy(() -> findingService.create(cmd)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesFinding() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(findingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpdateFindingCommand(
                    "Updated title", null, FindingSeverity.CRITICAL, null, null, null, null, false, false, false);
            var result = findingService.update(projectId, f.getId(), cmd);

            assertThat(result.getTitle()).isEqualTo("Updated title");
            assertThat(result.getSeverity()).isEqualTo(FindingSeverity.CRITICAL);
            // unchanged
            assertThat(result.getFindingType()).isEqualTo(FindingType.CONTROL_DEFICIENCY);
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(findingRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            var cmd = new UpdateFindingCommand("x", null, null, null, null, null, null, false, false, false);

            assertThatThrownBy(() -> findingService.update(projectId, id, cmd)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void rejectsBlankTitle() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));

            var cmd = new UpdateFindingCommand("   ", null, null, null, null, null, null, false, false, false);

            assertThatThrownBy(() -> findingService.update(projectId, f.getId(), cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("title");
        }

        @Test
        void rejectsBlankDescription() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));

            var cmd = new UpdateFindingCommand(null, null, null, "", null, null, null, false, false, false);

            assertThatThrownBy(() -> findingService.update(projectId, f.getId(), cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("description");
        }

        @Test
        void clearsRootCauseWhenFlagSet() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(findingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpdateFindingCommand(null, null, null, null, null, null, null, true, false, false);
            var result = findingService.update(projectId, f.getId(), cmd);

            assertThat(result.getRootCauseAnalysis()).isNull();
            assertThat(result.getOwner()).isEqualTo("alice");
        }

        @Test
        void clearsOwnerWhenFlagSet() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(findingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpdateFindingCommand(null, null, null, null, null, null, null, false, true, false);
            var result = findingService.update(projectId, f.getId(), cmd);

            assertThat(result.getOwner()).isNull();
            assertThat(result.getDueDate()).isEqualTo(DUE);
        }

        @Test
        void clearsDueDateWhenFlagSet() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(findingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpdateFindingCommand(null, null, null, null, null, null, null, false, false, true);
            var result = findingService.update(projectId, f.getId(), cmd);

            assertThat(result.getDueDate()).isNull();
            assertThat(result.getOwner()).isEqualTo("alice");
        }

        @Test
        void clearFlagOverridesProvidedValue() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(findingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpdateFindingCommand(
                    null, null, null, null, "ignored", "ignored-owner", LocalDate.of(2030, 1, 1), true, true, true);
            var result = findingService.update(projectId, f.getId(), cmd);

            assertThat(result.getRootCauseAnalysis()).isNull();
            assertThat(result.getOwner()).isNull();
            assertThat(result.getDueDate()).isNull();
        }
    }

    @Nested
    class TransitionStatus {

        @Test
        void transitionsOpenToRemediationInProgress() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(findingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = findingService.transitionStatus(projectId, f.getId(), FindingStatus.REMEDIATION_IN_PROGRESS);

            assertThat(result.getStatus()).isEqualTo(FindingStatus.REMEDIATION_IN_PROGRESS);
        }

        @Test
        void throwsOnInvalidTransition() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));

            assertThatThrownBy(
                            () -> findingService.transitionStatus(projectId, f.getId(), FindingStatus.VERIFIED_CLOSED))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsFinding() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));

            var result = findingService.getById(projectId, f.getId());

            assertThat(result.getUid()).isEqualTo("FIND-001");
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(findingRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findingService.getById(projectId, id)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class GetByUid {

        @Test
        void returnsFinding() {
            var f = makeFinding();
            when(findingRepository.findByProjectIdAndUid(projectId, "FIND-001")).thenReturn(Optional.of(f));

            var result = findingService.getByUid("FIND-001", projectId);

            assertThat(result.getId()).isEqualTo(f.getId());
        }

        @Test
        void throwsWhenNotFound() {
            when(findingRepository.findByProjectIdAndUid(projectId, "FIND-999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findingService.getByUid("FIND-999", projectId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ListByProject {

        @Test
        void listsFindings() {
            when(findingRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                    .thenReturn(List.of(makeFinding()));

            var result = findingService.listByProject(projectId);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesFindingWhenNoReverseLinks() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());
            when(controlLinkRepository.findControlUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            ControlLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());
            when(riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            RiskScenarioLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());
            when(findingLinkRepository.findByFindingId(f.getId())).thenReturn(List.of());

            findingService.delete(projectId, f.getId());

            verify(findingRepository).delete(f);
        }

        @Test
        void deletesOutboundLinksThroughRepositoryBeforeParent() {
            var f = makeFinding();
            var outboundLinks = List.of(
                    new com.keplerops.groundcontrol.domain.findings.model.FindingLink(
                            f,
                            com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType.CONTROL,
                            UUID.randomUUID(),
                            null,
                            com.keplerops.groundcontrol.domain.findings.state.FindingLinkType.MITIGATED_BY),
                    new com.keplerops.groundcontrol.domain.findings.model.FindingLink(
                            f,
                            com.keplerops.groundcontrol.domain.findings.state.FindingLinkTargetType.EVIDENCE,
                            null,
                            "s3://evidence/x",
                            com.keplerops.groundcontrol.domain.findings.state.FindingLinkType.EVIDENCED_BY));
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());
            when(controlLinkRepository.findControlUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            ControlLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());
            when(riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            RiskScenarioLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());
            when(findingLinkRepository.findByFindingId(f.getId())).thenReturn(outboundLinks);

            findingService.delete(projectId, f.getId());

            // Envers writes delete revisions only when Hibernate sees the link
            // delete. Driving the deletes through findingLinkRepository.deleteAll
            // is the contract that closes the parent-delete audit-history gap
            // (cycle-2 pre-push codex review on issue #279, ADR-038).
            var inOrder = org.mockito.Mockito.inOrder(findingLinkRepository, findingRepository);
            inOrder.verify(findingLinkRepository).deleteAll(outboundLinks);
            inOrder.verify(findingRepository).delete(f);
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(findingRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findingService.delete(projectId, id)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void rejectsDeleteWhenAssetLinkReferencesFinding() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of("ASSET-A", "ASSET-B"));
            when(controlLinkRepository.findControlUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            ControlLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());
            when(riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            RiskScenarioLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());

            var thrown =
                    catchThrowableOfType(() -> findingService.delete(projectId, f.getId()), ConflictException.class);
            assertThat(thrown).isNotNull().hasMessageContaining("reverse links");
            assertThat(thrown.getErrorCode()).isEqualTo("finding_referenced");
            var detail = thrown.getDetail();
            assertThat(detail).containsEntry("findingUid", f.getUid());
            assertThat(detail).containsEntry("assetCount", 2);
            assertThat(detail).containsEntry("controlCount", 0);
            assertThat(detail).containsEntry("scenarioCount", 0);
            assertThat(detail.get("assetUids")).isEqualTo(List.of("ASSET-A", "ASSET-B"));
        }

        @Test
        void rejectsDeleteWhenControlLinkReferencesFinding() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());
            when(controlLinkRepository.findControlUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            ControlLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of("CTRL-1"));
            when(riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            RiskScenarioLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());

            var thrown =
                    catchThrowableOfType(() -> findingService.delete(projectId, f.getId()), ConflictException.class);
            assertThat(thrown).isNotNull();
            assertThat(thrown.getDetail()).containsEntry("controlCount", 1);
            assertThat(thrown.getDetail().get("controlUids")).isEqualTo(List.of("CTRL-1"));
        }

        @Test
        void rejectsDeleteWhenRiskScenarioLinkReferencesFinding() {
            var f = makeFinding();
            when(findingRepository.findByIdAndProjectId(f.getId(), projectId)).thenReturn(Optional.of(f));
            when(assetLinkRepository.findAssetUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            AssetLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());
            when(controlLinkRepository.findControlUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            ControlLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of());
            when(riskScenarioLinkRepository.findRiskScenarioUidsByTargetTypeAndTargetEntityIdAndProjectId(
                            RiskScenarioLinkTargetType.FINDING, f.getId(), projectId))
                    .thenReturn(List.of("RS-001"));

            var thrown =
                    catchThrowableOfType(() -> findingService.delete(projectId, f.getId()), ConflictException.class);
            assertThat(thrown).isNotNull();
            assertThat(thrown.getDetail()).containsEntry("scenarioCount", 1);
            assertThat(thrown.getDetail().get("scenarioUids")).isEqualTo(List.of("RS-001"));
        }
    }
}
