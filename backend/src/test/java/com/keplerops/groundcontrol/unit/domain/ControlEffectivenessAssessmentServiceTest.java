package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlEffectivenessAssessment;
import com.keplerops.groundcontrol.domain.controls.repository.ControlEffectivenessAssessmentRepository;
import com.keplerops.groundcontrol.domain.controls.service.ControlEffectivenessAssessmentService;
import com.keplerops.groundcontrol.domain.controls.service.ControlService;
import com.keplerops.groundcontrol.domain.controls.service.CreateControlEffectivenessAssessmentCommand;
import com.keplerops.groundcontrol.domain.controls.service.UpdateControlEffectivenessAssessmentCommand;
import com.keplerops.groundcontrol.domain.controls.state.ControlEffectivenessRating;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
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
class ControlEffectivenessAssessmentServiceTest {

    @Mock
    private ControlEffectivenessAssessmentRepository repository;

    @Mock
    private com.keplerops.groundcontrol.domain.controls.repository.ControlTestRepository controlTestRepository;

    @Mock
    private ControlService controlService;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ControlEffectivenessAssessmentService service;

    private Project project;
    private UUID projectId;
    private Control control;
    private UUID controlId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
        control = new Control(project, "CTRL-001", "Access Control", ControlFunction.PREVENTIVE);
        controlId = UUID.randomUUID();
        setField(control, "id", controlId);
    }

    private CreateControlEffectivenessAssessmentCommand happyCommand() {
        return new CreateControlEffectivenessAssessmentCommand(
                projectId,
                controlId,
                "CEA-001",
                ControlEffectivenessRating.EFFECTIVE,
                ControlEffectivenessRating.PARTIALLY_EFFECTIVE,
                LocalDate.of(2026, 5, 1),
                "auditor@example.com",
                "Design good, occasional operating gaps.",
                "Q2 review",
                null);
    }

    private CreateControlEffectivenessAssessmentCommand happyCommandWithSupportingTests(java.util.List<UUID> testIds) {
        return new CreateControlEffectivenessAssessmentCommand(
                projectId,
                controlId,
                "CEA-001",
                ControlEffectivenessRating.EFFECTIVE,
                ControlEffectivenessRating.PARTIALLY_EFFECTIVE,
                LocalDate.of(2026, 5, 1),
                "auditor@example.com",
                "Design good, occasional operating gaps.",
                "Q2 review",
                testIds);
    }

    @Nested
    class Create {

        @Test
        void createsAssessmentWithBothRatings() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            when(repository.existsByProjectIdAndUid(projectId, "CEA-001")).thenReturn(false);
            when(repository.save(any(ControlEffectivenessAssessment.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.create(happyCommand());

            assertThat(result.getUid()).isEqualTo("CEA-001");
            assertThat(result.getDesignEffectiveness()).isEqualTo(ControlEffectivenessRating.EFFECTIVE);
            assertThat(result.getOperatingEffectiveness()).isEqualTo(ControlEffectivenessRating.PARTIALLY_EFFECTIVE);
            assertThat(result.getRationale()).isEqualTo("Design good, occasional operating gaps.");
            assertThat(result.getControl()).isSameAs(control);
        }

        @Test
        void rejectsDuplicateUid() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            when(repository.existsByProjectIdAndUid(projectId, "CEA-001")).thenReturn(true);

            var command = happyCommand();
            assertThatThrownBy(() -> service.create(command))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("CEA-001");
        }

        @Test
        void rejectsWhenControlInDifferentProject() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(controlService.getById(projectId, controlId))
                    .thenThrow(new NotFoundException("Control not found: " + controlId));

            var command = happyCommand();
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void persistsValidatedSupportingTestIds() {
            var testId = UUID.randomUUID();
            var testInProject = new com.keplerops.groundcontrol.domain.controls.model.ControlTest(
                    project,
                    control,
                    "CT-001",
                    com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology.INSPECTION,
                    com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion.EFFECTIVE,
                    "auditor",
                    LocalDate.of(2026, 5, 1));
            setField(testInProject, "id", testId);
            when(projectService.getById(projectId)).thenReturn(project);
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            when(repository.existsByProjectIdAndUid(projectId, "CEA-001")).thenReturn(false);
            when(controlTestRepository.findByIdAndProjectIdAndControlId(testId, projectId, controlId))
                    .thenReturn(Optional.of(testInProject));
            when(repository.save(any(ControlEffectivenessAssessment.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.create(happyCommandWithSupportingTests(java.util.List.of(testId)));

            assertThat(result.getSupportingTestIds()).containsExactly(testId.toString());
        }

        @Test
        void rejectsSupportingTestNotInProject() {
            var missingTestId = UUID.randomUUID();
            when(projectService.getById(projectId)).thenReturn(project);
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            when(repository.existsByProjectIdAndUid(projectId, "CEA-001")).thenReturn(false);
            when(controlTestRepository.findByIdAndProjectIdAndControlId(missingTestId, projectId, controlId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(happyCommandWithSupportingTests(java.util.List.of(missingTestId))))
                    .isInstanceOf(com.keplerops.groundcontrol.domain.exception.DomainValidationException.class)
                    .hasMessageContaining("supportingTestIds");
        }

        @Test
        void dedupesSupportingTestIdsPreservingOrder() {
            var testIdA = UUID.randomUUID();
            var testIdB = UUID.randomUUID();
            var testA = new com.keplerops.groundcontrol.domain.controls.model.ControlTest(
                    project,
                    control,
                    "CT-A",
                    com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology.INSPECTION,
                    com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion.EFFECTIVE,
                    "auditor",
                    LocalDate.of(2026, 5, 1));
            setField(testA, "id", testIdA);
            var testB = new com.keplerops.groundcontrol.domain.controls.model.ControlTest(
                    project,
                    control,
                    "CT-B",
                    com.keplerops.groundcontrol.domain.controls.state.ControlTestMethodology.INSPECTION,
                    com.keplerops.groundcontrol.domain.controls.state.ControlTestConclusion.EFFECTIVE,
                    "auditor",
                    LocalDate.of(2026, 5, 2));
            setField(testB, "id", testIdB);
            when(projectService.getById(projectId)).thenReturn(project);
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            when(repository.existsByProjectIdAndUid(projectId, "CEA-001")).thenReturn(false);
            when(controlTestRepository.findByIdAndProjectIdAndControlId(testIdA, projectId, controlId))
                    .thenReturn(Optional.of(testA));
            when(controlTestRepository.findByIdAndProjectIdAndControlId(testIdB, projectId, controlId))
                    .thenReturn(Optional.of(testB));
            when(repository.save(any(ControlEffectivenessAssessment.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.create(happyCommandWithSupportingTests(java.util.List.of(testIdA, testIdB, testIdA)));

            assertThat(result.getSupportingTestIds()).containsExactly(testIdA.toString(), testIdB.toString());
        }
    }

    @Nested
    class Update {

        @Test
        void updatesRatingsAndRationale() {
            var existing = existingAssessment();
            when(repository.findByIdAndProjectId(existing.getId(), projectId)).thenReturn(Optional.of(existing));
            when(repository.save(any(ControlEffectivenessAssessment.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateControlEffectivenessAssessmentCommand(
                    null,
                    ControlEffectivenessRating.INEFFECTIVE,
                    null,
                    null,
                    "Operating effectiveness regressed.",
                    null,
                    null);
            var result = service.update(projectId, existing.getId(), command);

            assertThat(result.getDesignEffectiveness()).isEqualTo(ControlEffectivenessRating.EFFECTIVE);
            assertThat(result.getOperatingEffectiveness()).isEqualTo(ControlEffectivenessRating.INEFFECTIVE);
            assertThat(result.getRationale()).isEqualTo("Operating effectiveness regressed.");
        }

        @Test
        void notFoundWhenWrongProject() {
            var id = UUID.randomUUID();
            when(repository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());
            var command = new UpdateControlEffectivenessAssessmentCommand(null, null, null, null, null, null, null);

            assertThatThrownBy(() -> service.update(projectId, id, command)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Reads {

        @Test
        void getByIdReturnsAssessment() {
            var existing = existingAssessment();
            when(repository.findByIdAndProjectId(existing.getId(), projectId)).thenReturn(Optional.of(existing));

            assertThat(service.getById(projectId, existing.getId())).isSameAs(existing);
        }

        @Test
        void getByIdNotFound() {
            var id = UUID.randomUUID();
            when(repository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(projectId, id)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void listByProjectDelegates() {
            var existing = existingAssessment();
            when(repository.findByProjectIdOrderByAssessedAtDesc(projectId)).thenReturn(List.of(existing));

            assertThat(service.listByProject(projectId)).containsExactly(existing);
        }

        @Test
        void listByProjectAndControlValidatesControl() {
            var existing = existingAssessment();
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            when(repository.findByProjectIdAndControlIdOrderByAssessedAtDesc(projectId, controlId))
                    .thenReturn(List.of(existing));

            assertThat(service.listByProjectAndControl(projectId, controlId)).containsExactly(existing);
        }
    }

    @Test
    void deleteRemovesAndLogs() {
        var existing = existingAssessment();
        when(repository.findByIdAndProjectId(existing.getId(), projectId)).thenReturn(Optional.of(existing));

        service.delete(projectId, existing.getId());

        verify(repository).delete(existing);
    }

    private ControlEffectivenessAssessment existingAssessment() {
        var assessment = new ControlEffectivenessAssessment(
                project,
                control,
                "CEA-001",
                ControlEffectivenessRating.EFFECTIVE,
                ControlEffectivenessRating.EFFECTIVE,
                LocalDate.of(2026, 5, 1),
                "auditor@example.com");
        setField(assessment, "id", UUID.randomUUID());
        return assessment;
    }
}
