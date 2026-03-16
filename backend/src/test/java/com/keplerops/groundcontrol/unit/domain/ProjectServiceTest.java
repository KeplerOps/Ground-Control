package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.repository.ProjectRepository;
import com.keplerops.groundcontrol.domain.projects.service.CreateProjectCommand;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.projects.service.UpdateProjectCommand;
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
class ProjectServiceTest {

    private static final UUID PROJECT_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @Mock
    private ProjectRepository projectRepository;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository);
    }

    private Project makeProject(String identifier, String name) {
        var project = new Project(identifier, name);
        try {
            Field field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    private Project makeProjectWithId(UUID id, String identifier, String name) {
        var project = new Project(identifier, name);
        try {
            Field field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return project;
    }

    @Nested
    class Create {

        @Test
        void createsProjectSuccessfully() {
            var command = new CreateProjectCommand("my-project", "My Project", "A description");
            when(projectRepository.existsByIdentifier("my-project")).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.create(command);
            assertThat(result.getIdentifier()).isEqualTo("my-project");
            assertThat(result.getName()).isEqualTo("My Project");
            assertThat(result.getDescription()).isEqualTo("A description");
        }

        @Test
        void createsProjectWithNullDescription() {
            var command = new CreateProjectCommand("my-project", "My Project", null);
            when(projectRepository.existsByIdentifier("my-project")).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.create(command);
            assertThat(result.getIdentifier()).isEqualTo("my-project");
        }

        @Test
        void throwsConflictForDuplicateIdentifier() {
            var command = new CreateProjectCommand("existing", "Existing", null);
            when(projectRepository.existsByIdentifier("existing")).thenReturn(true);

            assertThatThrownBy(() -> service.create(command)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsProjectWhenFound() {
            var project = makeProjectWithId(PROJECT_ID, "test", "Test");
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            var result = service.getById(PROJECT_ID);
            assertThat(result.getIdentifier()).isEqualTo("test");
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(PROJECT_ID)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class GetByIdentifier {

        @Test
        void returnsProjectWhenFound() {
            var project = makeProject("test", "Test");
            when(projectRepository.findByIdentifier("test")).thenReturn(Optional.of(project));

            var result = service.getByIdentifier("test");
            assertThat(result.getIdentifier()).isEqualTo("test");
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(projectRepository.findByIdentifier("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByIdentifier("missing")).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ListProjects {

        @Test
        void returnsAllProjects() {
            var projects = List.of(makeProject("p1", "Project 1"), makeProject("p2", "Project 2"));
            when(projectRepository.findAll()).thenReturn(projects);

            var result = service.list();
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class UpdateByIdentifier {

        @Test
        void updatesNameAndDescription() {
            var project = makeProject("test", "Old Name");
            when(projectRepository.findByIdentifier("test")).thenReturn(Optional.of(project));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateProjectCommand("New Name", "New Description");
            var result = service.updateByIdentifier("test", command);
            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getDescription()).isEqualTo("New Description");
        }

        @Test
        void nullFieldsAreNotUpdated() {
            var project = makeProject("test", "Old Name");
            project.setDescription("Old Desc");
            when(projectRepository.findByIdentifier("test")).thenReturn(Optional.of(project));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateProjectCommand(null, null);
            var result = service.updateByIdentifier("test", command);
            assertThat(result.getName()).isEqualTo("Old Name");
            assertThat(result.getDescription()).isEqualTo("Old Desc");
        }
    }

    @Nested
    class Update {

        @Test
        void updatesById() {
            var project = makeProjectWithId(PROJECT_ID, "test", "Old Name");
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateProjectCommand("New Name", "New Desc");
            var result = service.update(PROJECT_ID, command);
            assertThat(result.getName()).isEqualTo("New Name");
        }
    }

    @Nested
    class ResolveProject {

        @Test
        void resolvesWithExplicitIdentifier() {
            var project = makeProject("ground-control", "Ground Control");
            when(projectRepository.findByIdentifier("ground-control")).thenReturn(Optional.of(project));

            var result = service.resolveProject("ground-control");
            assertThat(result.getIdentifier()).isEqualTo("ground-control");
        }

        @Test
        void resolvesWhenNullAndSingleProject() {
            var project = makeProject("only-one", "Only One");
            when(projectRepository.count()).thenReturn(1L);
            when(projectRepository.findAll()).thenReturn(List.of(project));

            var result = service.resolveProject(null);
            assertThat(result.getIdentifier()).isEqualTo("only-one");
        }

        @Test
        void throwsWhenNullAndMultipleProjects() {
            when(projectRepository.count()).thenReturn(2L);

            assertThatThrownBy(() -> service.resolveProject(null)).isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class ResolveProjectId {

        @Test
        void returnsProjectId() {
            var project = makeProjectWithId(PROJECT_ID, "ground-control", "Ground Control");
            when(projectRepository.findByIdentifier("ground-control")).thenReturn(Optional.of(project));

            var result = service.resolveProjectId("ground-control");
            assertThat(result).isEqualTo(PROJECT_ID);
        }
    }
}
