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
import com.keplerops.groundcontrol.domain.testcases.model.TestCase;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseStep;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseStepRepository;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestCaseStepCommand;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseStepService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestCaseStepCommand;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
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
class TestCaseStepServiceTest {

    @Mock
    private TestCaseStepRepository stepRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private TestCaseStepService stepService;

    private Project project;
    private TestCase testCase;
    private UUID projectId;
    private UUID testCaseId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
        testCase = new TestCase(project, "TC-001", "Login flow", TestCaseType.MANUAL, TestCasePriority.HIGH);
        testCaseId = UUID.randomUUID();
        setField(testCase, "id", testCaseId);
    }

    private TestCaseStep makeStep(int stepNumber) {
        var step = new TestCaseStep(testCase, stepNumber, "Open page", "Page renders");
        setField(step, "id", UUID.randomUUID());
        return step;
    }

    @Nested
    class Create {

        @Test
        void createsStep() {
            when(testCaseRepository.findByIdAndProjectId(testCaseId, projectId)).thenReturn(Optional.of(testCase));
            when(stepRepository.existsByTestCaseIdAndStepNumber(testCaseId, 1)).thenReturn(false);
            when(stepRepository.save(any(TestCaseStep.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateTestCaseStepCommand(projectId, testCaseId, 1, "act", "exp", null);
            var result = stepService.create(command);

            assertThat(result.getStepNumber()).isEqualTo(1);
            assertThat(result.getAction()).isEqualTo("act");
            assertThat(result.getExpectedResult()).isEqualTo("exp");
            assertThat(result.getActualResult()).isNull();
            assertThat(result.getTestCase()).isSameAs(testCase);
        }

        @Test
        void createsStepWithActualResult() {
            when(testCaseRepository.findByIdAndProjectId(testCaseId, projectId)).thenReturn(Optional.of(testCase));
            when(stepRepository.existsByTestCaseIdAndStepNumber(testCaseId, 2)).thenReturn(false);
            when(stepRepository.save(any(TestCaseStep.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateTestCaseStepCommand(projectId, testCaseId, 2, "act", "exp", "observed");
            var result = stepService.create(command);

            assertThat(result.getActualResult()).isEqualTo("observed");
        }

        @Test
        void rejectsCreateWhenTestCaseNotInProject() {
            when(testCaseRepository.findByIdAndProjectId(testCaseId, projectId)).thenReturn(Optional.empty());

            var command = new CreateTestCaseStepCommand(projectId, testCaseId, 1, "act", "exp", null);

            assertThatThrownBy(() -> stepService.create(command))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(testCaseId.toString());
        }

        @Test
        void rejectsDuplicateStepNumber() {
            when(testCaseRepository.findByIdAndProjectId(testCaseId, projectId)).thenReturn(Optional.of(testCase));
            when(stepRepository.existsByTestCaseIdAndStepNumber(testCaseId, 1)).thenReturn(true);

            var command = new CreateTestCaseStepCommand(projectId, testCaseId, 1, "act", "exp", null);

            assertThatThrownBy(() -> stepService.create(command))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Step number 1");
        }

        @Test
        void rejectsNonPositiveStepNumber() {
            when(testCaseRepository.findByIdAndProjectId(testCaseId, projectId)).thenReturn(Optional.of(testCase));

            var command = new CreateTestCaseStepCommand(projectId, testCaseId, 0, "act", "exp", null);

            assertThatThrownBy(() -> stepService.create(command)).isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesActionAndExpectedResult() {
            var step = makeStep(1);
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            when(stepRepository.findByIdAndTestCaseId(step.getId(), testCaseId)).thenReturn(Optional.of(step));
            when(stepRepository.save(any(TestCaseStep.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateTestCaseStepCommand(null, "new action", "new expected", null, false);
            var result = stepService.update(projectId, testCaseId, step.getId(), command);

            assertThat(result.getAction()).isEqualTo("new action");
            assertThat(result.getExpectedResult()).isEqualTo("new expected");
        }

        @Test
        void updatesStepNumberWhenUnique() {
            var step = makeStep(1);
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            when(stepRepository.findByIdAndTestCaseId(step.getId(), testCaseId)).thenReturn(Optional.of(step));
            when(stepRepository.existsByTestCaseIdAndStepNumber(testCaseId, 5)).thenReturn(false);
            when(stepRepository.save(any(TestCaseStep.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateTestCaseStepCommand(5, null, null, null, false);
            var result = stepService.update(projectId, testCaseId, step.getId(), command);

            assertThat(result.getStepNumber()).isEqualTo(5);
        }

        @Test
        void rejectsDuplicateStepNumberOnUpdate() {
            var step = makeStep(1);
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            when(stepRepository.findByIdAndTestCaseId(step.getId(), testCaseId)).thenReturn(Optional.of(step));
            when(stepRepository.existsByTestCaseIdAndStepNumber(testCaseId, 5)).thenReturn(true);

            var command = new UpdateTestCaseStepCommand(5, null, null, null, false);

            assertThatThrownBy(() -> stepService.update(projectId, testCaseId, step.getId(), command))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Step number 5");
        }

        @Test
        void allowsKeepingSameStepNumber() {
            var step = makeStep(3);
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            when(stepRepository.findByIdAndTestCaseId(step.getId(), testCaseId)).thenReturn(Optional.of(step));
            when(stepRepository.save(any(TestCaseStep.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateTestCaseStepCommand(3, "tweak", null, null, false);
            var result = stepService.update(projectId, testCaseId, step.getId(), command);

            assertThat(result.getStepNumber()).isEqualTo(3);
            assertThat(result.getAction()).isEqualTo("tweak");
        }

        @Test
        void clearsActualResultWhenFlagSet() {
            var step = makeStep(1);
            step.setActualResult("previously observed");
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            when(stepRepository.findByIdAndTestCaseId(step.getId(), testCaseId)).thenReturn(Optional.of(step));
            when(stepRepository.save(any(TestCaseStep.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateTestCaseStepCommand(null, null, null, null, true);
            var result = stepService.update(projectId, testCaseId, step.getId(), command);

            assertThat(result.getActualResult()).isNull();
        }

        @Test
        void setsActualResultWhenProvided() {
            var step = makeStep(1);
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            when(stepRepository.findByIdAndTestCaseId(step.getId(), testCaseId)).thenReturn(Optional.of(step));
            when(stepRepository.save(any(TestCaseStep.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateTestCaseStepCommand(null, null, null, "observed", false);
            var result = stepService.update(projectId, testCaseId, step.getId(), command);

            assertThat(result.getActualResult()).isEqualTo("observed");
        }

        @Test
        void rejectsUpdateWhenTestCaseNotInProject() {
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(false);

            var command = new UpdateTestCaseStepCommand(null, "act", null, null, false);

            assertThatThrownBy(() -> stepService.update(projectId, testCaseId, UUID.randomUUID(), command))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void rejectsUpdateWhenStepNotInTestCase() {
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            var stepId = UUID.randomUUID();
            when(stepRepository.findByIdAndTestCaseId(stepId, testCaseId)).thenReturn(Optional.empty());

            var command = new UpdateTestCaseStepCommand(null, "act", null, null, false);

            assertThatThrownBy(() -> stepService.update(projectId, testCaseId, stepId, command))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(stepId.toString());
        }
    }

    @Nested
    class GetAndList {

        @Test
        void getByIdReturnsStep() {
            var step = makeStep(1);
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            when(stepRepository.findByIdAndTestCaseId(step.getId(), testCaseId)).thenReturn(Optional.of(step));

            var result = stepService.getById(projectId, testCaseId, step.getId());

            assertThat(result).isSameAs(step);
        }

        @Test
        void getByIdRejectsCrossProjectAccess() {
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(false);

            assertThatThrownBy(() -> stepService.getById(projectId, testCaseId, UUID.randomUUID()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void listByTestCaseReturnsOrderedSteps() {
            var step1 = makeStep(1);
            var step2 = makeStep(2);
            var step3 = makeStep(3);
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            when(stepRepository.findByTestCaseIdOrderByStepNumberAsc(testCaseId))
                    .thenReturn(List.of(step1, step2, step3));

            var result = stepService.listByTestCase(projectId, testCaseId);

            assertThat(result).extracting(TestCaseStep::getStepNumber).containsExactly(1, 2, 3);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesStep() {
            var step = makeStep(1);
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            when(stepRepository.findByIdAndTestCaseId(step.getId(), testCaseId)).thenReturn(Optional.of(step));

            stepService.delete(projectId, testCaseId, step.getId());

            verify(stepRepository).delete(step);
        }

        @Test
        void rejectsDeleteWhenStepNotInTestCase() {
            when(testCaseRepository.existsByIdAndProjectId(testCaseId, projectId))
                    .thenReturn(true);
            var stepId = UUID.randomUUID();
            when(stepRepository.findByIdAndTestCaseId(stepId, testCaseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stepService.delete(projectId, testCaseId, stepId))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
