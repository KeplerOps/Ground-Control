package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.baselines.model.Baseline;
import com.keplerops.groundcontrol.domain.baselines.repository.BaselineRepository;
import com.keplerops.groundcontrol.domain.baselines.service.BaselineService;
import com.keplerops.groundcontrol.domain.baselines.service.CreateBaselineCommand;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaselineServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID BASELINE_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");

    @Mock
    private BaselineRepository baselineRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query nativeQuery;

    private BaselineService service;

    @BeforeEach
    void setUp() {
        service = new BaselineService(baselineRepository, projectRepository, entityManager);
    }

    private Project makeProject() {
        var project = new Project("test-project", "Test Project");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    private Baseline makeBaseline(String name, int revisionNumber) {
        var project = makeProject();
        var baseline = new Baseline(project, name, "desc", revisionNumber, "test-actor");
        setField(baseline, "id", BASELINE_ID);
        return baseline;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class Create {

        @Test
        void createsBaselineSuccessfully() {
            var project = makeProject();
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(baselineRepository.existsByProjectIdAndName(PROJECT_ID, "v1.0"))
                    .thenReturn(false);
            when(entityManager.createNativeQuery("SELECT COALESCE(MAX(rev), 0) FROM revinfo"))
                    .thenReturn(nativeQuery);
            when(nativeQuery.getSingleResult()).thenReturn(42);
            when(baselineRepository.save(any(Baseline.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateBaselineCommand(PROJECT_ID, "v1.0", "First release");
            var result = service.create(command);

            assertThat(result.getName()).isEqualTo("v1.0");
            assertThat(result.getDescription()).isEqualTo("First release");
            assertThat(result.getRevisionNumber()).isEqualTo(42);
            assertThat(result.getProject().getId()).isEqualTo(PROJECT_ID);
        }

        @Test
        void throwsConflictForDuplicateName() {
            var project = makeProject();
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(baselineRepository.existsByProjectIdAndName(PROJECT_ID, "v1.0"))
                    .thenReturn(true);

            var command = new CreateBaselineCommand(PROJECT_ID, "v1.0", "duplicate");
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(ConflictException.class);
        }

        @Test
        void throwsNotFoundForMissingProject() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            var command = new CreateBaselineCommand(PROJECT_ID, "v1.0", null);
            assertThatThrownBy(() -> service.create(command)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsBaselineWhenFound() {
            var baseline = makeBaseline("v1.0", 10);
            when(baselineRepository.findById(BASELINE_ID)).thenReturn(Optional.of(baseline));

            var result = service.getById(BASELINE_ID);
            assertThat(result.getName()).isEqualTo("v1.0");
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(baselineRepository.findById(BASELINE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(BASELINE_ID)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ListByProject {

        @Test
        void returnsBaselinesForProject() {
            var b1 = makeBaseline("v1.0", 10);
            var b2 = makeBaseline("v2.0", 20);
            when(baselineRepository.findByProjectIdOrderByCreatedAtDesc(PROJECT_ID))
                    .thenReturn(List.of(b2, b1));

            var result = service.listByProject(PROJECT_ID);
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesBaselineSuccessfully() {
            var baseline = makeBaseline("v1.0", 10);
            when(baselineRepository.findById(BASELINE_ID)).thenReturn(Optional.of(baseline));

            service.delete(BASELINE_ID);
            verify(baselineRepository).delete(baseline);
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(baselineRepository.findById(BASELINE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(BASELINE_ID)).isInstanceOf(NotFoundException.class);
        }
    }
}
