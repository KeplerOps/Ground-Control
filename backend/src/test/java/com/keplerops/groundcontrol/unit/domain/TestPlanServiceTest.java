package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.testcases.model.TestPlan;
import com.keplerops.groundcontrol.domain.testcases.repository.TestPlanRepository;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestPlanCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestPlanService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestPlanCommand;
import com.keplerops.groundcontrol.domain.testcases.state.TestPlanStatus;
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
class TestPlanServiceTest {

    @Mock
    private TestPlanRepository testPlanRepository;

    @Mock
    private com.keplerops.groundcontrol.domain.testcases.repository.TestRunRepository testRunRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private TestPlanService testPlanService;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private TestPlan plan(String uid) {
        var plan = new TestPlan(project, uid, "Plan " + uid);
        setField(plan, "id", UUID.randomUUID());
        return plan;
    }

    @Nested
    class Create {

        @Test
        void createsPlanWithRequiredFields() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testPlanRepository.existsByProjectIdAndUid(projectId, "TP-001"))
                    .thenReturn(false);
            when(testPlanRepository.save(any(TestPlan.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = testPlanService.create(new CreateTestPlanCommand(
                    projectId,
                    "TP-001",
                    "Wave-1 acceptance",
                    "scope notes",
                    "ground-control",
                    "1.2.0",
                    "build-42",
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30)));

            assertThat(result.getUid()).isEqualTo("TP-001");
            assertThat(result.getName()).isEqualTo("Wave-1 acceptance");
            assertThat(result.getDescription()).isEqualTo("scope notes");
            assertThat(result.getProduct()).isEqualTo("ground-control");
            assertThat(result.getVersion()).isEqualTo("1.2.0");
            assertThat(result.getBuild()).isEqualTo("build-42");
            assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
            assertThat(result.getStatus()).isEqualTo(TestPlanStatus.DRAFT);
        }

        @Test
        void rejectsDuplicateUidWithConflict() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testPlanRepository.existsByProjectIdAndUid(projectId, "TP-001"))
                    .thenReturn(true);

            var command = new CreateTestPlanCommand(projectId, "TP-001", "Plan", null, null, null, null, null, null);
            assertThatThrownBy(() -> testPlanService.create(command))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("TP-001");
            verify(testPlanRepository, never()).save(any());
        }

        @Test
        void rejectsEndDateBeforeStartDate() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(testPlanRepository.existsByProjectIdAndUid(projectId, "TP-001"))
                    .thenReturn(false);

            var command = new CreateTestPlanCommand(
                    projectId,
                    "TP-001",
                    "Plan",
                    null,
                    null,
                    null,
                    null,
                    LocalDate.of(2026, 6, 30),
                    LocalDate.of(2026, 6, 1));
            assertThatThrownBy(() -> testPlanService.create(command)).isInstanceOf(DomainValidationException.class);
            verify(testPlanRepository, never()).save(any());
        }
    }

    @Nested
    class Reads {

        @Test
        void getByIdReturnsExistingPlan() {
            var existing = plan("TP-001");
            when(testPlanRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));

            assertThat(testPlanService.getById(projectId, existing.getId())).isSameAs(existing);
        }

        @Test
        void getByIdThrowsNotFoundWhenMissing() {
            var missingId = UUID.randomUUID();
            when(testPlanRepository.findByIdAndProjectId(missingId, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testPlanService.getById(projectId, missingId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(missingId.toString());
        }

        @Test
        void getByUidReturnsExistingPlan() {
            var existing = plan("TP-001");
            when(testPlanRepository.findByProjectIdAndUid(projectId, "TP-001")).thenReturn(Optional.of(existing));

            assertThat(testPlanService.getByUid(projectId, "TP-001")).isSameAs(existing);
        }

        @Test
        void getByUidThrowsNotFoundWhenMissing() {
            when(testPlanRepository.findByProjectIdAndUid(projectId, "TP-XYZ")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testPlanService.getByUid(projectId, "TP-XYZ"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("TP-XYZ");
        }

        @Test
        void listByProjectDelegatesToRepository() {
            var a = plan("TP-001");
            var b = plan("TP-002");
            when(testPlanRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                    .thenReturn(List.of(a, b));

            assertThat(testPlanService.listByProject(projectId)).containsExactly(a, b);
        }
    }

    @Nested
    class Updates {

        @Test
        void partialUpdatePreservesUntouchedFields() {
            var existing = plan("TP-001");
            existing.setDescription("original description");
            existing.setProduct("ground-control");
            existing.setVersion("1.0.0");
            existing.setBuild("old-build");
            existing.setStartDate(LocalDate.of(2026, 6, 1));
            existing.setEndDate(LocalDate.of(2026, 6, 30));
            when(testPlanRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));
            when(testPlanRepository.save(any(TestPlan.class))).thenAnswer(inv -> inv.getArgument(0));

            // name change only — every other field stays the same.
            var updated = testPlanService.update(
                    projectId,
                    existing.getId(),
                    new UpdateTestPlanCommand(
                            "Renamed", null, null, null, null, null, null, false, false, false, false, false, false));

            assertThat(updated.getName()).isEqualTo("Renamed");
            assertThat(updated.getDescription()).isEqualTo("original description");
            assertThat(updated.getProduct()).isEqualTo("ground-control");
            assertThat(updated.getVersion()).isEqualTo("1.0.0");
            assertThat(updated.getBuild()).isEqualTo("old-build");
            assertThat(updated.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(updated.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        }

        @Test
        void clearFlagsExplicitlyClearNullableFields() {
            var existing = plan("TP-001");
            existing.setDescription("desc");
            existing.setProduct("product");
            existing.setVersion("version");
            existing.setBuild("build");
            existing.setStartDate(LocalDate.of(2026, 6, 1));
            existing.setEndDate(LocalDate.of(2026, 6, 30));
            when(testPlanRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));
            when(testPlanRepository.save(any(TestPlan.class))).thenAnswer(inv -> inv.getArgument(0));

            var updated = testPlanService.update(
                    projectId,
                    existing.getId(),
                    new UpdateTestPlanCommand(
                            null, null, null, null, null, null, null, true, true, true, true, true, true));

            assertThat(updated.getDescription()).isNull();
            assertThat(updated.getProduct()).isNull();
            assertThat(updated.getVersion()).isNull();
            assertThat(updated.getBuild()).isNull();
            assertThat(updated.getStartDate()).isNull();
            assertThat(updated.getEndDate()).isNull();
        }

        @Test
        void updateThrowsNotFoundWhenMissing() {
            var missingId = UUID.randomUUID();
            when(testPlanRepository.findByIdAndProjectId(missingId, projectId)).thenReturn(Optional.empty());

            var command = new UpdateTestPlanCommand(
                    "Renamed", null, null, null, null, null, null, false, false, false, false, false, false);
            assertThatThrownBy(() -> testPlanService.update(projectId, missingId, command))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void updateAcceptsWholeWindowShiftPastExistingEndDate() {
            // Class fix from codex cycle 1: existing window 6/1–6/30, client
            // moves the whole window forward to 7/1–7/31. The naive
            // setter-at-a-time path would reject this because setStartDate(7/1)
            // compares 7/1 to the still-old 6/30 endDate. The service must
            // validate the final pair once and reassign cleanly.
            var existing = plan("TP-001");
            existing.setStartDate(LocalDate.of(2026, 6, 1));
            existing.setEndDate(LocalDate.of(2026, 6, 30));
            when(testPlanRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));
            when(testPlanRepository.save(any(TestPlan.class))).thenAnswer(inv -> inv.getArgument(0));

            var updated = testPlanService.update(
                    projectId,
                    existing.getId(),
                    new UpdateTestPlanCommand(
                            null,
                            null,
                            null,
                            null,
                            null,
                            LocalDate.of(2026, 7, 1),
                            LocalDate.of(2026, 7, 31),
                            false,
                            false,
                            false,
                            false,
                            false,
                            false));

            assertThat(updated.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(updated.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        }

        @Test
        void updateAcceptsWholeWindowShiftEarlier() {
            // Mirror of the forward shift: backwards-shifted window. Without
            // the final-pair validation, setEndDate(4/30) would compare 4/30
            // to the still-old 6/1 startDate and reject.
            var existing = plan("TP-001");
            existing.setStartDate(LocalDate.of(2026, 6, 1));
            existing.setEndDate(LocalDate.of(2026, 6, 30));
            when(testPlanRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));
            when(testPlanRepository.save(any(TestPlan.class))).thenAnswer(inv -> inv.getArgument(0));

            var updated = testPlanService.update(
                    projectId,
                    existing.getId(),
                    new UpdateTestPlanCommand(
                            null,
                            null,
                            null,
                            null,
                            null,
                            LocalDate.of(2026, 4, 1),
                            LocalDate.of(2026, 4, 30),
                            false,
                            false,
                            false,
                            false,
                            false,
                            false));

            assertThat(updated.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(updated.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        }

        @Test
        void updateRejectsScheduleInversion() {
            // Date setters validate ordering against the *current* state, so
            // updating start_date alone to a value past the stored end_date
            // must surface as DomainValidationException rather than silently
            // succeed.
            var existing = plan("TP-001");
            existing.setStartDate(LocalDate.of(2026, 6, 1));
            existing.setEndDate(LocalDate.of(2026, 6, 30));
            when(testPlanRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));

            var planId = existing.getId();
            var command = new UpdateTestPlanCommand(
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.of(2026, 7, 15),
                    null,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false);
            assertThatThrownBy(() -> testPlanService.update(projectId, planId, command))
                    .isInstanceOf(DomainValidationException.class);
            verify(testPlanRepository, never()).save(any());
        }
    }

    @Nested
    class StatusTransition {

        @Test
        void transitionApplied() {
            var existing = plan("TP-001");
            when(testPlanRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));
            when(testPlanRepository.save(any(TestPlan.class))).thenAnswer(inv -> inv.getArgument(0));

            var transitioned = testPlanService.transitionStatus(projectId, existing.getId(), TestPlanStatus.ACTIVE);

            assertThat(transitioned.getStatus()).isEqualTo(TestPlanStatus.ACTIVE);
        }

        @Test
        void illegalTransitionSurfacesAsDomainValidation() {
            var existing = plan("TP-001");
            when(testPlanRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));

            var planId = existing.getId();
            assertThatThrownBy(() -> testPlanService.transitionStatus(projectId, planId, TestPlanStatus.COMPLETED))
                    .isInstanceOf(DomainValidationException.class);
            verify(testPlanRepository, never()).save(any());
        }

        @Test
        void transitionThrowsNotFoundWhenMissing() {
            var missingId = UUID.randomUUID();
            when(testPlanRepository.findByIdAndProjectId(missingId, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testPlanService.transitionStatus(projectId, missingId, TestPlanStatus.ACTIVE))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Delete {

        @Test
        void deleteExistingPlan() {
            var existing = plan("TP-001");
            when(testPlanRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));

            testPlanService.delete(projectId, existing.getId());

            verify(testPlanRepository).delete(existing);
        }

        @Test
        void deleteThrowsNotFoundWhenMissing() {
            var missingId = UUID.randomUUID();
            when(testPlanRepository.findByIdAndProjectId(missingId, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testPlanService.delete(projectId, missingId))
                    .isInstanceOf(NotFoundException.class);
            verify(testPlanRepository, never()).delete(any(TestPlan.class));
        }

        @Test
        void deleteRejectsConflictWhenTestRunsReferenceThePlan() {
            // TC-008 / ADR-049: existence check raises a domain-aware
            // ConflictException instead of letting the FK violation surface
            // as a late DataIntegrityViolationException.
            var existing = plan("TP-001");
            when(testPlanRepository.findByIdAndProjectId(existing.getId(), projectId))
                    .thenReturn(Optional.of(existing));
            when(testRunRepository.existsByTestPlanId(existing.getId())).thenReturn(true);

            assertThatThrownBy(() -> testPlanService.delete(projectId, existing.getId()))
                    .isInstanceOf(com.keplerops.groundcontrol.domain.exception.ConflictException.class)
                    .hasMessageContaining("associated test runs");
            verify(testPlanRepository, never()).delete(any(TestPlan.class));
        }
    }
}
