package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.model.ControlLink;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.service.ControlLinkService;
import com.keplerops.groundcontrol.domain.controls.service.ControlService;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkType;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
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
class ControlLinkServiceTest {

    @Mock
    private ControlLinkRepository controlLinkRepository;

    @Mock
    private ControlService controlService;

    @InjectMocks
    private ControlLinkService controlLinkService;

    private UUID projectId;
    private UUID controlId;
    private Control control;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        controlId = UUID.randomUUID();
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", projectId);
        control = new Control(project, "CTRL-001", "Access Control", ControlFunction.PREVENTIVE);
        setField(control, "id", controlId);
    }

    @Nested
    class Create {

        @Test
        void createsLinkWithExternalIdentifier() {
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            when(controlLinkRepository.existsByControlIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            controlId, ControlLinkTargetType.ASSET, "ASSET-001", ControlLinkType.PROTECTS))
                    .thenReturn(false);
            when(controlLinkRepository.save(any(ControlLink.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = controlLinkService.create(
                    projectId,
                    controlId,
                    ControlLinkTargetType.ASSET,
                    null,
                    "ASSET-001",
                    ControlLinkType.PROTECTS,
                    null,
                    null);

            assertThat(result.getTargetType()).isEqualTo(ControlLinkTargetType.ASSET);
            assertThat(result.getTargetIdentifier()).isEqualTo("ASSET-001");
            assertThat(result.getLinkType()).isEqualTo(ControlLinkType.PROTECTS);
        }

        @Test
        void throwsOnDuplicateLink() {
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            when(controlLinkRepository.existsByControlIdAndTargetTypeAndTargetIdentifierAndLinkType(
                            controlId, ControlLinkTargetType.EXTERNAL, "ext-1", ControlLinkType.ASSOCIATED))
                    .thenReturn(true);

            assertThatThrownBy(() -> controlLinkService.create(
                            projectId,
                            controlId,
                            ControlLinkTargetType.EXTERNAL,
                            null,
                            "ext-1",
                            ControlLinkType.ASSOCIATED,
                            null,
                            null))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class ListByControl {

        @Test
        void listsAllLinks() {
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            var link = new ControlLink(control, ControlLinkTargetType.ASSET, null, "A-1", ControlLinkType.PROTECTS);
            when(controlLinkRepository.findByControlId(controlId)).thenReturn(List.of(link));

            var result = controlLinkService.listByControl(projectId, controlId, null);

            assertThat(result).hasSize(1);
        }

        @Test
        void filtersLinksByTargetType() {
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            when(controlLinkRepository.findByControlIdAndTargetType(controlId, ControlLinkTargetType.EVIDENCE))
                    .thenReturn(List.of());

            var result = controlLinkService.listByControl(projectId, controlId, ControlLinkTargetType.EVIDENCE);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesLink() {
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            var linkId = UUID.randomUUID();
            var link = new ControlLink(control, ControlLinkTargetType.ASSET, null, "A-1", ControlLinkType.PROTECTS);
            setField(link, "id", linkId);
            when(controlLinkRepository.findByIdAndControlProjectId(linkId, projectId))
                    .thenReturn(Optional.of(link));

            controlLinkService.delete(projectId, controlId, linkId);

            verify(controlLinkRepository).delete(link);
        }

        @Test
        void throwsWhenLinkNotFound() {
            when(controlService.getById(projectId, controlId)).thenReturn(control);
            var linkId = UUID.randomUUID();
            when(controlLinkRepository.findByIdAndControlProjectId(linkId, projectId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> controlLinkService.delete(projectId, controlId, linkId))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
