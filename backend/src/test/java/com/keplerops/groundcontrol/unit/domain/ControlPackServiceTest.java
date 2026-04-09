package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPack;
import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPackEntry;
import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPackOverride;
import com.keplerops.groundcontrol.domain.controlpacks.repository.ControlPackEntryRepository;
import com.keplerops.groundcontrol.domain.controlpacks.repository.ControlPackOverrideRepository;
import com.keplerops.groundcontrol.domain.controlpacks.repository.ControlPackRepository;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackService;
import com.keplerops.groundcontrol.domain.controlpacks.service.CreateControlPackOverrideCommand;
import com.keplerops.groundcontrol.domain.controlpacks.service.InstallControlPackCommand;
import com.keplerops.groundcontrol.domain.controlpacks.service.UpgradeControlPackCommand;
import com.keplerops.groundcontrol.domain.controlpacks.state.ControlPackEntryStatus;
import com.keplerops.groundcontrol.domain.controls.model.Control;
import com.keplerops.groundcontrol.domain.controls.repository.ControlLinkRepository;
import com.keplerops.groundcontrol.domain.controls.repository.ControlRepository;
import com.keplerops.groundcontrol.domain.controls.state.ControlFunction;
import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ControlPackServiceTest {

    @Mock
    private ControlPackRepository controlPackRepository;

    @Mock
    private ControlPackEntryRepository entryRepository;

    @Mock
    private ControlPackOverrideRepository overrideRepository;

    @Mock
    private ControlRepository controlRepository;

    @Mock
    private ControlLinkRepository controlLinkRepository;

    @Mock
    private ProjectService projectService;

    private ControlPackService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PACK_UUID = UUID.fromString("00000000-0000-0000-0000-000000000050");
    private static final UUID CONTROL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000052");
    private static final UUID ENTRY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000051");

    private Project project;

    @BeforeEach
    void setUp() {
        service = new ControlPackService(
                controlPackRepository,
                entryRepository,
                overrideRepository,
                controlRepository,
                controlLinkRepository,
                projectService);
        project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
    }

    private ControlPackEntryDefinition entryDef(String uid, String title) {
        return new ControlPackEntryDefinition(
                uid,
                title,
                "desc",
                "obj",
                ControlFunction.PREVENTIVE,
                "owner",
                null,
                null,
                null,
                "Access Control",
                "NIST",
                null,
                null,
                null);
    }

    private InstallControlPackCommand installCmd(String packId, String version, ControlPackEntryDefinition... entries) {
        return new InstallControlPackCommand(
                PROJECT_ID, packId, version, "NIST", "Test pack", null, null, null, null, List.of(entries));
    }

    private ControlPack makePack(String packId, String version) {
        var pack = new ControlPack(project, packId, version);
        setField(pack, "id", PACK_UUID);
        setField(pack, "createdAt", Instant.now());
        setField(pack, "updatedAt", Instant.now());
        return pack;
    }

    private Control makeControl(String uid, String title) {
        var control = new Control(project, uid, title, ControlFunction.PREVENTIVE);
        setField(control, "id", CONTROL_UUID);
        setField(control, "createdAt", Instant.now());
        setField(control, "updatedAt", Instant.now());
        return control;
    }

    @Nested
    class Install {

        @Test
        void createsPackAndMaterializesControls() {
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "test-pack"))
                    .thenReturn(Optional.empty());
            when(controlPackRepository.save(any())).thenAnswer(inv -> {
                var pack = inv.<ControlPack>getArgument(0);
                setField(pack, "id", PACK_UUID);
                setField(pack, "createdAt", Instant.now());
                setField(pack, "updatedAt", Instant.now());
                return pack;
            });
            when(controlRepository.findByProjectIdAndUid(PROJECT_ID, "AC-1")).thenReturn(Optional.empty());
            when(controlRepository.save(any())).thenAnswer(inv -> {
                var ctrl = inv.<Control>getArgument(0);
                setField(ctrl, "id", UUID.randomUUID());
                setField(ctrl, "createdAt", Instant.now());
                setField(ctrl, "updatedAt", Instant.now());
                return ctrl;
            });
            when(entryRepository.existsByControlPackIdAndEntryUid(any(), any())).thenReturn(false);
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.install(installCmd("test-pack", "1.0.0", entryDef("AC-1", "Access Control")));

            assertThat(result.controlsCreated()).isEqualTo(1);
            assertThat(result.entriesCreated()).isEqualTo(1);
            assertThat(result.wasIdempotent()).isFalse();
        }

        @Test
        void idempotentWhenSameVersionAlreadyInstalled() {
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            var existingPack = makePack("test-pack", "1.0.0");
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "test-pack"))
                    .thenReturn(Optional.of(existingPack));
            when(entryRepository.existsByControlPackIdAndEntryUid(PACK_UUID, "AC-1"))
                    .thenReturn(true);

            var result = service.install(installCmd("test-pack", "1.0.0", entryDef("AC-1", "Access Control")));

            assertThat(result.wasIdempotent()).isTrue();
            assertThat(result.controlsCreated()).isEqualTo(0);
        }

        @Test
        void throwsConflictWhenDifferentVersionInstalled() {
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            var existingPack = makePack("test-pack", "1.0.0");
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "test-pack"))
                    .thenReturn(Optional.of(existingPack));

            assertThatThrownBy(
                            () -> service.install(installCmd("test-pack", "2.0.0", entryDef("AC-1", "Access Control"))))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Use upgrade");
        }

        @Test
        void linksExistingControlWithoutOverwriting() {
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "test-pack"))
                    .thenReturn(Optional.empty());
            when(controlPackRepository.save(any())).thenAnswer(inv -> {
                var pack = inv.<ControlPack>getArgument(0);
                setField(pack, "id", PACK_UUID);
                setField(pack, "createdAt", Instant.now());
                setField(pack, "updatedAt", Instant.now());
                return pack;
            });
            var existingControl = makeControl("AC-1", "Existing Title");
            existingControl.setSource("manual");
            when(controlRepository.findByProjectIdAndUid(PROJECT_ID, "AC-1")).thenReturn(Optional.of(existingControl));
            when(entryRepository.existsByControlPackIdAndEntryUid(any(), any())).thenReturn(false);
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.install(installCmd("test-pack", "1.0.0", entryDef("AC-1", "New Title")));

            assertThat(result.controlsLinked()).isEqualTo(1);
            assertThat(result.controlsCreated()).isEqualTo(0);
            // Source should not be overwritten since it was already set
            assertThat(existingControl.getSource()).isEqualTo("manual");
        }
    }

    @Nested
    class Upgrade {

        @Test
        void appliesUpstreamChangesToNonOverriddenFields() {
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            var pack = makePack("test-pack", "1.0.0");
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "test-pack"))
                    .thenReturn(Optional.of(pack));

            var control = makeControl("AC-1", "Old Title");
            var entry = new ControlPackEntry(pack, control, "AC-1");
            setField(entry, "id", ENTRY_UUID);
            setField(entry, "createdAt", Instant.now());
            setField(entry, "updatedAt", Instant.now());
            entry.setOriginalDefinition(Map.of("uid", "AC-1", "title", "Old Title", "category", "Access Control"));

            when(entryRepository.findByControlPackId(PACK_UUID)).thenReturn(List.of(entry));
            when(overrideRepository.findByControlPackEntryId(ENTRY_UUID)).thenReturn(List.of());
            when(controlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(controlPackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpgradeControlPackCommand(
                    PROJECT_ID,
                    "test-pack",
                    "2.0.0",
                    "NIST",
                    "desc",
                    null,
                    null,
                    null,
                    null,
                    List.of(entryDef("AC-1", "New Title")));

            var result = service.upgrade(cmd);

            assertThat(result.controlsUpdated()).isEqualTo(1);
            assertThat(result.entriesUpdated()).isEqualTo(1);
            assertThat(control.getTitle()).isEqualTo("New Title");
        }

        @Test
        void preservesLocalOverrides() {
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            var pack = makePack("test-pack", "1.0.0");
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "test-pack"))
                    .thenReturn(Optional.of(pack));

            var control = makeControl("AC-1", "Custom Title");
            var entry = new ControlPackEntry(pack, control, "AC-1");
            setField(entry, "id", ENTRY_UUID);
            setField(entry, "createdAt", Instant.now());
            setField(entry, "updatedAt", Instant.now());
            entry.setOriginalDefinition(Map.of("uid", "AC-1", "title", "Old Title"));

            var override = new ControlPackOverride(entry, "title", "Custom Title", "Local policy");

            when(entryRepository.findByControlPackId(PACK_UUID)).thenReturn(List.of(entry));
            when(overrideRepository.findByControlPackEntryId(ENTRY_UUID)).thenReturn(List.of(override));
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(controlPackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new UpgradeControlPackCommand(
                    PROJECT_ID,
                    "test-pack",
                    "2.0.0",
                    "NIST",
                    "desc",
                    null,
                    null,
                    null,
                    null,
                    List.of(entryDef("AC-1", "New Title")));

            var result = service.upgrade(cmd);

            assertThat(result.overridesPreserved()).isEqualTo(1);
            // Title should remain as override, not the upstream change
            assertThat(control.getTitle()).isEqualTo("Custom Title");
        }

        @Test
        void deprecatesRemovedEntries() {
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            var pack = makePack("test-pack", "1.0.0");
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "test-pack"))
                    .thenReturn(Optional.of(pack));

            var control = makeControl("AC-1", "Access Control");
            var entry = new ControlPackEntry(pack, control, "AC-1");
            setField(entry, "id", ENTRY_UUID);
            setField(entry, "createdAt", Instant.now());
            setField(entry, "updatedAt", Instant.now());
            entry.setOriginalDefinition(Map.of("uid", "AC-1", "title", "Access Control"));

            when(entryRepository.findByControlPackId(PACK_UUID)).thenReturn(List.of(entry));
            when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(controlPackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Upgrade with empty entries list (AC-1 removed)
            var cmd = new UpgradeControlPackCommand(
                    PROJECT_ID,
                    "test-pack",
                    "2.0.0",
                    "NIST",
                    "desc",
                    null,
                    null,
                    null,
                    null,
                    List.of(entryDef("AC-2", "New Control")));

            // Mock for the new entry being added
            when(controlRepository.findByProjectIdAndUid(PROJECT_ID, "AC-2")).thenReturn(Optional.empty());
            when(controlRepository.save(any())).thenAnswer(inv -> {
                var ctrl = inv.<Control>getArgument(0);
                setField(ctrl, "id", UUID.randomUUID());
                setField(ctrl, "createdAt", Instant.now());
                setField(ctrl, "updatedAt", Instant.now());
                return ctrl;
            });
            when(entryRepository.existsByControlPackIdAndEntryUid(PACK_UUID, "AC-2"))
                    .thenReturn(false);

            var result = service.upgrade(cmd);

            assertThat(result.entriesDeprecated()).isEqualTo(1);
            assertThat(entry.getEntryStatus()).isEqualTo(ControlPackEntryStatus.DEPRECATED);
        }

        @Test
        void throwsNotFoundWhenPackNotInstalled() {
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "missing-pack"))
                    .thenReturn(Optional.empty());

            var cmd = new UpgradeControlPackCommand(
                    PROJECT_ID,
                    "missing-pack",
                    "2.0.0",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(entryDef("AC-1", "Test")));

            assertThatThrownBy(() -> service.upgrade(cmd)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class Tailoring {

        @Test
        void appliesOverrideToControl() {
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "test-pack"))
                    .thenReturn(Optional.of(makePack("test-pack", "1.0.0")));
            var control = makeControl("AC-1", "Original Title");
            var entry = new ControlPackEntry(makePack("test-pack", "1.0.0"), control, "AC-1");
            setField(entry, "id", ENTRY_UUID);
            setField(entry, "createdAt", Instant.now());
            setField(entry, "updatedAt", Instant.now());
            entry.setOriginalDefinition(Map.of("title", "Original Title"));

            when(entryRepository.findByControlPackIdAndEntryUid(PACK_UUID, "AC-1"))
                    .thenReturn(Optional.of(entry));
            when(overrideRepository.findByControlPackEntryIdAndFieldName(ENTRY_UUID, "title"))
                    .thenReturn(Optional.empty());
            when(overrideRepository.save(any())).thenAnswer(inv -> {
                var o = inv.<ControlPackOverride>getArgument(0);
                setField(o, "id", UUID.randomUUID());
                setField(o, "createdAt", Instant.now());
                setField(o, "updatedAt", Instant.now());
                return o;
            });
            when(controlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var cmd = new CreateControlPackOverrideCommand("title", "Custom Title", "Local policy");
            service.createOverride(PROJECT_ID, "test-pack", "AC-1", cmd);

            assertThat(control.getTitle()).isEqualTo("Custom Title");
        }

        @Test
        void rejectsInvalidFieldName() {
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "test-pack"))
                    .thenReturn(Optional.of(makePack("test-pack", "1.0.0")));
            var control = makeControl("AC-1", "Title");
            var entry = new ControlPackEntry(makePack("test-pack", "1.0.0"), control, "AC-1");
            setField(entry, "id", ENTRY_UUID);
            setField(entry, "createdAt", Instant.now());
            setField(entry, "updatedAt", Instant.now());
            entry.setOriginalDefinition(Map.of());

            when(entryRepository.findByControlPackIdAndEntryUid(PACK_UUID, "AC-1"))
                    .thenReturn(Optional.of(entry));

            var cmd = new CreateControlPackOverrideCommand("status", "ACTIVE", "Reason");

            assertThatThrownBy(() -> service.createOverride(PROJECT_ID, "test-pack", "AC-1", cmd))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Cannot override field");
        }

        @Test
        void deleteOverrideRestoresOriginalValue() {
            when(controlPackRepository.findByProjectIdAndPackId(PROJECT_ID, "test-pack"))
                    .thenReturn(Optional.of(makePack("test-pack", "1.0.0")));
            var control = makeControl("AC-1", "Custom Title");
            var entry = new ControlPackEntry(makePack("test-pack", "1.0.0"), control, "AC-1");
            setField(entry, "id", ENTRY_UUID);
            setField(entry, "createdAt", Instant.now());
            setField(entry, "updatedAt", Instant.now());
            entry.setOriginalDefinition(Map.of("title", "Original Title"));

            var override = new ControlPackOverride(entry, "title", "Custom Title", "Reason");
            var overrideId = UUID.randomUUID();
            setField(override, "id", overrideId);

            when(entryRepository.findByControlPackIdAndEntryUid(PACK_UUID, "AC-1"))
                    .thenReturn(Optional.of(entry));
            when(overrideRepository.findById(overrideId)).thenReturn(Optional.of(override));
            when(controlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.deleteOverride(PROJECT_ID, "test-pack", "AC-1", overrideId);

            assertThat(control.getTitle()).isEqualTo("Original Title");
            verify(overrideRepository).delete(override);
        }
    }
}
