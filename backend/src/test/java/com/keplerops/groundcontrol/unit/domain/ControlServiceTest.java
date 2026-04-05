package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.repository.ControlRepository;
import com.keplerops.groundcontrol.domain.controls.service.ControlService;
import com.keplerops.groundcontrol.domain.controls.service.CreateControlCommand;
import com.keplerops.groundcontrol.domain.controls.service.UpdateControlCommand;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlStatus;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
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
class ControlServiceTest {

    @Mock
    private ControlRepository controlRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ControlService controlService;

    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        project = new Project("ground-control", "Ground Control");
        projectId = UUID.randomUUID();
        setField(project, "id", projectId);
    }

    private Control makeControl() {
        var control = new Control(project, "CTRL-001", "Access Control", ControlFunction.PREVENTIVE);
        control.setDescription("Network access control");
        control.setOwner("Security Team");
        setField(control, "id", UUID.randomUUID());
        return control;
    }

    @Nested
    class Create {

        @Test
        void createsControl() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(controlRepository.existsByProjectIdAndUid(projectId, "CTRL-001"))
                    .thenReturn(false);
            when(controlRepository.save(any(Control.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new CreateControlCommand(
                    projectId,
                    "CTRL-001",
                    "Access Control",
                    ControlFunction.PREVENTIVE,
                    "desc",
                    "obj",
                    "owner",
                    "scope",
                    null,
                    null,
                    "Access Control",
                    "ISO 27001");
            var result = controlService.create(command);

            assertThat(result.getUid()).isEqualTo("CTRL-001");
            assertThat(result.getTitle()).isEqualTo("Access Control");
            assertThat(result.getControlFunction()).isEqualTo(ControlFunction.PREVENTIVE);
        }

        @Test
        void throwsOnDuplicateUid() {
            when(projectService.getById(projectId)).thenReturn(project);
            when(controlRepository.existsByProjectIdAndUid(projectId, "CTRL-001"))
                    .thenReturn(true);

            var command = new CreateControlCommand(
                    projectId,
                    "CTRL-001",
                    "title",
                    ControlFunction.DETECTIVE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            assertThatThrownBy(() -> controlService.create(command)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesControl() {
            var control = makeControl();
            when(controlRepository.findByIdAndProjectId(control.getId(), projectId))
                    .thenReturn(Optional.of(control));
            when(controlRepository.save(any(Control.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdateControlCommand(
                    "Updated Title", ControlFunction.DETECTIVE, null, null, null, null, null, null, null, null);
            var result = controlService.update(projectId, control.getId(), command);

            assertThat(result.getTitle()).isEqualTo("Updated Title");
            assertThat(result.getControlFunction()).isEqualTo(ControlFunction.DETECTIVE);
            assertThat(result.getDescription()).isEqualTo("Network access control");
        }
    }

    @Nested
    class TransitionStatus {

        @Test
        void transitionsDraftToProposed() {
            var control = makeControl();
            when(controlRepository.findByIdAndProjectId(control.getId(), projectId))
                    .thenReturn(Optional.of(control));
            when(controlRepository.save(any(Control.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = controlService.transitionStatus(projectId, control.getId(), ControlStatus.PROPOSED);

            assertThat(result.getStatus()).isEqualTo(ControlStatus.PROPOSED);
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsControl() {
            var control = makeControl();
            when(controlRepository.findByIdAndProjectId(control.getId(), projectId))
                    .thenReturn(Optional.of(control));

            var result = controlService.getById(projectId, control.getId());

            assertThat(result.getUid()).isEqualTo("CTRL-001");
        }

        @Test
        void throwsWhenNotFound() {
            var id = UUID.randomUUID();
            when(controlRepository.findByIdAndProjectId(id, projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controlService.getById(projectId, id)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ListByProject {

        @Test
        void listsControls() {
            when(controlRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                    .thenReturn(List.of(makeControl()));

            var result = controlService.listByProject(projectId);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesControl() {
            var control = makeControl();
            when(controlRepository.findByIdAndProjectId(control.getId(), projectId))
                    .thenReturn(Optional.of(control));

            controlService.delete(projectId, control.getId());

            verify(controlRepository).delete(control);
        }
    }
}
