package com.keplerops.groundcontrol.unit.domain;

import static com.keplerops.groundcontrol.TestUtil.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.repository.PackRegistryEntryRepository;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistryService;
import com.keplerops.groundcontrol.domain.packregistry.service.RegisterPackCommand;
import com.keplerops.groundcontrol.domain.packregistry.service.UpdatePackRegistryEntryCommand;
import com.keplerops.groundcontrol.domain.packregistry.state.CatalogStatus;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
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
class PackRegistryServiceTest {

    @Mock
    private PackRegistryEntryRepository registryRepository;

    @Mock
    private ProjectService projectService;

    private PackRegistryService service;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Project makeProject() {
        var project = new Project("ground-control", "Ground Control");
        setField(project, "id", PROJECT_ID);
        return project;
    }

    @BeforeEach
    void setUp() {
        service = new PackRegistryService(registryRepository, projectService);
    }

    @Nested
    class Register {

        @Test
        void registersNewEntry() {
            var project = makeProject();
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(registryRepository.existsByProjectIdAndPackIdAndVersion(PROJECT_ID, "nist-800-53", "1.0.0"))
                    .thenReturn(false);
            when(registryRepository.save(any(PackRegistryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new RegisterPackCommand(
                    PROJECT_ID,
                    "nist-800-53",
                    PackType.CONTROL_PACK,
                    "1.0.0",
                    "NIST",
                    "NIST controls",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            var result = service.registerEntry(command);
            assertThat(result.getPackId()).isEqualTo("nist-800-53");
            assertThat(result.getVersion()).isEqualTo("1.0.0");
            assertThat(result.getPackType()).isEqualTo(PackType.CONTROL_PACK);
            assertThat(result.getCatalogStatus()).isEqualTo(CatalogStatus.AVAILABLE);
            verify(registryRepository).save(any(PackRegistryEntry.class));
        }

        @Test
        void rejectsDuplicateVersion() {
            var project = makeProject();
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(registryRepository.existsByProjectIdAndPackIdAndVersion(PROJECT_ID, "nist-800-53", "1.0.0"))
                    .thenReturn(true);

            var command = new RegisterPackCommand(
                    PROJECT_ID,
                    "nist-800-53",
                    PackType.CONTROL_PACK,
                    "1.0.0",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            assertThatThrownBy(() -> service.registerEntry(command)).isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class Withdraw {

        @Test
        void withdrawsEntry() {
            var project = makeProject();
            var entry = new PackRegistryEntry(project, "nist-800-53", PackType.CONTROL_PACK, "1.0.0");
            setField(entry, "id", UUID.randomUUID());
            when(registryRepository.findByProjectIdAndPackIdAndVersion(PROJECT_ID, "nist-800-53", "1.0.0"))
                    .thenReturn(Optional.of(entry));
            when(registryRepository.save(any(PackRegistryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = service.withdrawEntry(PROJECT_ID, "nist-800-53", "1.0.0");
            assertThat(result.getCatalogStatus()).isEqualTo(CatalogStatus.WITHDRAWN);
        }
    }

    @Nested
    class Find {

        @Test
        void findsExistingEntry() {
            var project = makeProject();
            var entry = new PackRegistryEntry(project, "nist-800-53", PackType.CONTROL_PACK, "1.0.0");
            when(registryRepository.findByProjectIdAndPackIdAndVersion(PROJECT_ID, "nist-800-53", "1.0.0"))
                    .thenReturn(Optional.of(entry));

            var result = service.findEntry(PROJECT_ID, "nist-800-53", "1.0.0");
            assertThat(result.getPackId()).isEqualTo("nist-800-53");
        }

        @Test
        void throwsNotFoundForMissingEntry() {
            when(registryRepository.findByProjectIdAndPackIdAndVersion(PROJECT_ID, "missing", "1.0.0"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findEntry(PROJECT_ID, "missing", "1.0.0"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ListEntries {

        @Test
        void listsAllEntriesForProject() {
            var project = makeProject();
            var entry = new PackRegistryEntry(project, "nist-800-53", PackType.CONTROL_PACK, "1.0.0");
            when(registryRepository.findByProjectIdOrderByRegisteredAtDesc(PROJECT_ID))
                    .thenReturn(List.of(entry));

            var results = service.listEntries(PROJECT_ID);
            assertThat(results).hasSize(1);
        }

        @Test
        void listsEntriesByType() {
            var project = makeProject();
            var entry = new PackRegistryEntry(project, "nist-800-53", PackType.CONTROL_PACK, "1.0.0");
            when(registryRepository.findByProjectIdAndPackTypeOrderByRegisteredAtDesc(
                            PROJECT_ID, PackType.CONTROL_PACK))
                    .thenReturn(List.of(entry));

            var results = service.listEntries(PROJECT_ID, PackType.CONTROL_PACK);
            assertThat(results).hasSize(1);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesExistingEntry() {
            var project = makeProject();
            var entry = new PackRegistryEntry(project, "nist-800-53", PackType.CONTROL_PACK, "1.0.0");
            var entryId = UUID.randomUUID();
            setField(entry, "id", entryId);
            when(registryRepository.findById(entryId)).thenReturn(Optional.of(entry));
            when(registryRepository.save(any(PackRegistryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new UpdatePackRegistryEntryCommand(
                    "Updated Publisher", "New description", null, null, null, null, null, null, null);

            var result = service.updateEntry(entryId, command);
            assertThat(result.getPublisher()).isEqualTo("Updated Publisher");
            assertThat(result.getDescription()).isEqualTo("New description");
        }
    }
}
