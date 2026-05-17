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
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseGherkin;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseGherkinRepository;
import com.keplerops.groundcontrol.domain.testcases.repository.TestCaseRepository;
import com.keplerops.groundcontrol.domain.testcases.service.CreateTestCaseGherkinCommand;
import com.keplerops.groundcontrol.domain.testcases.service.GherkinValidator;
import com.keplerops.groundcontrol.domain.testcases.service.TestCaseGherkinService;
import com.keplerops.groundcontrol.domain.testcases.service.UpdateTestCaseGherkinCommand;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseFormat;
import com.keplerops.groundcontrol.domain.testcases.state.TestCasePriority;
import com.keplerops.groundcontrol.domain.testcases.state.TestCaseType;
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
class TestCaseGherkinServiceTest {

    @Mock
    private TestCaseGherkinRepository gherkinRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private GherkinValidator validator;

    @InjectMocks
    private TestCaseGherkinService service;

    private Project project;
    private UUID projectId;
    private TestCase gherkinTestCase;
    private TestCase stepTestCase;

    private static final String VALID_SOURCE =
            """
            Feature: Sign in

              Scenario: ok
                Given the user signs in
                Then they land on the dashboard
            """;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);

        gherkinTestCase = new TestCase(
                project, "TC-G01", "Sign in", TestCaseType.MANUAL, TestCasePriority.HIGH, TestCaseFormat.GHERKIN);
        setField(gherkinTestCase, "id", UUID.randomUUID());

        stepTestCase = new TestCase(
                project, "TC-S01", "Step-based", TestCaseType.MANUAL, TestCasePriority.HIGH, TestCaseFormat.STEP_BASED);
        setField(stepTestCase, "id", UUID.randomUUID());
    }

    @Nested
    class Create {

        @Test
        void persistsValidatedGherkinSource() {
            var id = gherkinTestCase.getId();
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.of(gherkinTestCase));
            when(gherkinRepository.existsByTestCaseId(id)).thenReturn(false);
            when(gherkinRepository.save(any(TestCaseGherkin.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.create(new CreateTestCaseGherkinCommand(projectId, id, VALID_SOURCE));

            assertThat(result.getSource()).isEqualTo(VALID_SOURCE);
            assertThat(result.getTestCase()).isSameAs(gherkinTestCase);
            verify(validator).validate(VALID_SOURCE);
        }

        @Test
        void rejectsCreateWhenFormatIsStepBased() {
            var id = stepTestCase.getId();
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.of(stepTestCase));
            var command = new CreateTestCaseGherkinCommand(projectId, id, VALID_SOURCE);

            assertThatThrownBy(() -> service.create(command))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("GHERKIN");
        }

        @Test
        void rejectsDuplicateGherkinPerTestCase() {
            var id = gherkinTestCase.getId();
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.of(gherkinTestCase));
            when(gherkinRepository.existsByTestCaseId(id)).thenReturn(true);
            var command = new CreateTestCaseGherkinCommand(projectId, id, VALID_SOURCE);

            assertThatThrownBy(() -> service.create(command)).isInstanceOf(ConflictException.class);
        }

        @Test
        void rejectsCreateWhenParentNotInProject() {
            var id = UUID.randomUUID();
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());
            var command = new CreateTestCaseGherkinCommand(projectId, id, VALID_SOURCE);

            assertThatThrownBy(() -> service.create(command)).isInstanceOf(NotFoundException.class);
        }

        @Test
        void delegatesParseFailureToValidator() {
            var id = gherkinTestCase.getId();
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.of(gherkinTestCase));
            when(gherkinRepository.existsByTestCaseId(id)).thenReturn(false);
            org.mockito.Mockito.doThrow(
                            new DomainValidationException("bad", "invalid_gherkin_source", java.util.Map.of()))
                    .when(validator)
                    .validate("garbage");
            var command = new CreateTestCaseGherkinCommand(projectId, id, "garbage");

            assertThatThrownBy(() -> service.create(command)).isInstanceOf(DomainValidationException.class);
            verify(gherkinRepository, org.mockito.Mockito.never()).save(any(TestCaseGherkin.class));
        }
    }

    @Nested
    class Update {

        @Test
        void replacesValidatedSource() {
            var id = gherkinTestCase.getId();
            var existing = new TestCaseGherkin(gherkinTestCase, "old");
            setField(existing, "id", UUID.randomUUID());
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.of(gherkinTestCase));
            when(gherkinRepository.findByTestCaseId(id)).thenReturn(Optional.of(existing));
            when(gherkinRepository.save(any(TestCaseGherkin.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.update(projectId, id, new UpdateTestCaseGherkinCommand(VALID_SOURCE));

            assertThat(result.getSource()).isEqualTo(VALID_SOURCE);
            verify(validator).validate(VALID_SOURCE);
        }

        @Test
        void rejectsWhenGherkinAbsent() {
            var id = gherkinTestCase.getId();
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.of(gherkinTestCase));
            when(gherkinRepository.findByTestCaseId(id)).thenReturn(Optional.empty());
            var command = new UpdateTestCaseGherkinCommand(VALID_SOURCE);

            assertThatThrownBy(() -> service.update(projectId, id, command)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class GetByTestCase {

        @Test
        void returnsGherkin() {
            var id = gherkinTestCase.getId();
            var existing = new TestCaseGherkin(gherkinTestCase, VALID_SOURCE);
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.of(gherkinTestCase));
            when(gherkinRepository.findByTestCaseId(id)).thenReturn(Optional.of(existing));

            assertThat(service.getByTestCase(projectId, id)).isSameAs(existing);
        }

        @Test
        void throwsWhenParentMissing() {
            var id = UUID.randomUUID();
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByTestCase(projectId, id)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class DeleteByTestCase {

        @Test
        void deletesExistingGherkin() {
            var id = gherkinTestCase.getId();
            var existing = new TestCaseGherkin(gherkinTestCase, VALID_SOURCE);
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.of(gherkinTestCase));
            when(gherkinRepository.findByTestCaseId(id)).thenReturn(Optional.of(existing));

            service.deleteByTestCase(projectId, id);

            verify(gherkinRepository).delete(existing);
        }

        @Test
        void throwsWhenGherkinAbsent() {
            var id = gherkinTestCase.getId();
            when(testCaseRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.of(gherkinTestCase));
            when(gherkinRepository.findByTestCaseId(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteByTestCase(projectId, id)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class CascadeDeleteByTestCase {

        @Test
        void delegatesToRepositoryBulkDelete() {
            var id = UUID.randomUUID();
            when(gherkinRepository.deleteAllByTestCaseId(id)).thenReturn(1L);

            assertThat(service.cascadeDeleteByTestCase(id)).isEqualTo(1L);
        }

        @Test
        void returnsZeroWhenNoGherkinExisted() {
            var id = UUID.randomUUID();
            when(gherkinRepository.deleteAllByTestCaseId(id)).thenReturn(0L);

            assertThat(service.cascadeDeleteByTestCase(id)).isZero();
        }
    }
}
