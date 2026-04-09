package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.plugins.model.RegisteredPlugin;
import com.keplerops.groundcontrol.domain.plugins.repository.RegisteredPluginRepository;
import com.keplerops.groundcontrol.domain.plugins.service.Plugin;
import com.keplerops.groundcontrol.domain.plugins.service.PluginDescriptor;
import com.keplerops.groundcontrol.domain.plugins.service.PluginRegistry;
import com.keplerops.groundcontrol.domain.plugins.service.RegisterPluginCommand;
import com.keplerops.groundcontrol.domain.plugins.state.PluginLifecycleState;
import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PluginRegistryTest {

    @Mock
    private RegisteredPluginRepository registeredPluginRepository;

    @Mock
    private ProjectService projectService;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    static class StubPlugin implements Plugin {

        private final PluginDescriptor descriptor;
        private final boolean available;
        private boolean initializeCalled;
        private boolean startCalled;

        StubPlugin(PluginDescriptor descriptor, boolean available) {
            this.descriptor = descriptor;
            this.available = available;
        }

        @Override
        public PluginDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public void initialize() {
            initializeCalled = true;
        }

        @Override
        public void start() {
            startCalled = true;
        }
    }

    static class FailingPlugin implements Plugin {

        @Override
        public PluginDescriptor descriptor() {
            return new PluginDescriptor(
                    "failing-plugin", "1.0.0", "Fails during init", PluginType.CUSTOM, Set.of(), Map.of());
        }

        @Override
        public void initialize() {
            throw new RuntimeException("init failed");
        }
    }

    private StubPlugin makePlugin(String name, PluginType type) {
        return new StubPlugin(
                new PluginDescriptor(name, "1.0.0", "Test plugin: " + name, type, Set.of("test"), Map.of()), true);
    }

    @Nested
    class ClasspathDiscovery {

        @Test
        void registersClasspathPluginsOnInit() {
            var plugin = makePlugin("builtin-verifier", PluginType.VERIFIER);
            var registry = new PluginRegistry(List.of(plugin), registeredPluginRepository, projectService);
            registry.initializeClasspathPlugins();

            when(registeredPluginRepository.findAll()).thenReturn(List.of());

            var plugins = registry.listPlugins();
            assertThat(plugins).hasSize(1);
            assertThat(plugins.get(0).name()).isEqualTo("builtin-verifier");
            assertThat(plugins.get(0).builtin()).isTrue();
            assertThat(plugins.get(0).state()).isEqualTo(PluginLifecycleState.STARTED);
        }

        @Test
        void callsLifecycleMethodsOnInit() {
            var plugin = makePlugin("lifecycle-test", PluginType.CUSTOM);
            var registry = new PluginRegistry(List.of(plugin), registeredPluginRepository, projectService);
            registry.initializeClasspathPlugins();

            assertThat(plugin.initializeCalled).isTrue();
            assertThat(plugin.startCalled).isTrue();
        }

        @Test
        void handlesEmptyPluginList() {
            var registry = new PluginRegistry(List.of(), registeredPluginRepository, projectService);
            registry.initializeClasspathPlugins();

            when(registeredPluginRepository.findAll()).thenReturn(List.of());

            assertThat(registry.listPlugins()).isEmpty();
        }

        @Test
        void failedPluginGetsFailedState() {
            var plugin = new FailingPlugin();
            var registry = new PluginRegistry(List.of(plugin), registeredPluginRepository, projectService);
            registry.initializeClasspathPlugins();

            when(registeredPluginRepository.findAll()).thenReturn(List.of());

            var plugins = registry.listPlugins();
            assertThat(plugins).hasSize(1);
            assertThat(plugins.get(0).state()).isEqualTo(PluginLifecycleState.FAILED);
        }

        @Test
        void duplicateClasspathPluginSkipped() {
            var plugin1 = makePlugin("dup-plugin", PluginType.CUSTOM);
            var plugin2 = makePlugin("dup-plugin", PluginType.CUSTOM);
            var registry = new PluginRegistry(List.of(plugin1, plugin2), registeredPluginRepository, projectService);
            registry.initializeClasspathPlugins();

            when(registeredPluginRepository.findAll()).thenReturn(List.of());

            assertThat(registry.listPlugins()).hasSize(1);
        }
    }

    @Nested
    class Lookup {

        private PluginRegistry registry;

        @BeforeEach
        void setUp() {
            var verifier = makePlugin("my-verifier", PluginType.VERIFIER);
            var validator = makePlugin("my-validator", PluginType.VALIDATOR);
            registry = new PluginRegistry(List.of(verifier, validator), registeredPluginRepository, projectService);
            registry.initializeClasspathPlugins();
        }

        @Test
        void getPluginByName() {
            var info = registry.getPlugin("my-verifier");
            assertThat(info.name()).isEqualTo("my-verifier");
            assertThat(info.type()).isEqualTo(PluginType.VERIFIER);
        }

        @Test
        void getPluginByNameThrowsForUnknown() {
            when(registeredPluginRepository.findByName("nonexistent")).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> registry.getPlugin("nonexistent")).isInstanceOf(NotFoundException.class);
        }

        @Test
        void listByTypeFiltersCorrectly() {
            when(registeredPluginRepository.findByPluginType(PluginType.VERIFIER))
                    .thenReturn(List.of());

            var verifiers = registry.listByType(PluginType.VERIFIER);
            assertThat(verifiers).hasSize(1);
            assertThat(verifiers.get(0).name()).isEqualTo("my-verifier");
        }

        @Test
        void listByCapabilityFiltersCorrectly() {
            when(registeredPluginRepository.findAll()).thenReturn(List.of());

            var withTest = registry.listByCapability("test");
            assertThat(withTest).hasSize(2);
        }

        @Test
        void listByCapabilityReturnsEmptyForUnknown() {
            when(registeredPluginRepository.findAll()).thenReturn(List.of());

            var results = registry.listByCapability("nonexistent-capability");
            assertThat(results).isEmpty();
        }
    }

    @Nested
    class DynamicRegistration {

        private PluginRegistry registry;

        @BeforeEach
        void setUp() {
            registry = new PluginRegistry(List.of(), registeredPluginRepository, projectService);
            registry.initializeClasspathPlugins();
        }

        @Test
        void registersPersistsToDB() {
            var project = new Project("ground-control", "Ground Control");
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(registeredPluginRepository.existsByProjectIdAndName(PROJECT_ID, "dynamic-handler"))
                    .thenReturn(false);

            var entity = new RegisteredPlugin(project, "dynamic-handler", "2.0.0", PluginType.PACK_HANDLER);
            entity.setDescription("A dynamic pack handler");
            entity.setCapabilities(Set.of("requirements-pack"));
            entity.setLifecycleState(PluginLifecycleState.STARTED);
            when(registeredPluginRepository.save(any())).thenReturn(entity);

            var command = new RegisterPluginCommand(
                    PROJECT_ID,
                    "dynamic-handler",
                    "2.0.0",
                    "A dynamic pack handler",
                    PluginType.PACK_HANDLER,
                    Set.of("requirements-pack"),
                    Map.of());
            var info = registry.registerPlugin(command);

            assertThat(info.name()).isEqualTo("dynamic-handler");
            assertThat(info.builtin()).isFalse();
            assertThat(info.type()).isEqualTo(PluginType.PACK_HANDLER);
            verify(registeredPluginRepository).save(any());
        }

        @Test
        void registerThrowsOnBuiltinNameConflict() {
            var builtin = makePlugin("conflict-name", PluginType.CUSTOM);
            registry = new PluginRegistry(List.of(builtin), registeredPluginRepository, projectService);
            registry.initializeClasspathPlugins();

            var command = new RegisterPluginCommand(
                    PROJECT_ID, "conflict-name", "1.0.0", "desc", PluginType.CUSTOM, Set.of(), Map.of());

            assertThatThrownBy(() -> registry.registerPlugin(command)).isInstanceOf(ConflictException.class);
        }

        @Test
        void registerThrowsOnDuplicateDynamicName() {
            var project = new Project("ground-control", "Ground Control");
            when(projectService.getById(PROJECT_ID)).thenReturn(project);
            when(registeredPluginRepository.existsByProjectIdAndName(PROJECT_ID, "dup-dynamic"))
                    .thenReturn(true);

            var command = new RegisterPluginCommand(
                    PROJECT_ID, "dup-dynamic", "1.0.0", "desc", PluginType.CUSTOM, Set.of(), Map.of());

            assertThatThrownBy(() -> registry.registerPlugin(command)).isInstanceOf(ConflictException.class);
        }

        @Test
        void unregisterDeletesFromDB() {
            var project = new Project("ground-control", "Ground Control");
            var entity = new RegisteredPlugin(project, "to-remove", "1.0.0", PluginType.CUSTOM);
            when(registeredPluginRepository.findByProjectIdAndName(PROJECT_ID, "to-remove"))
                    .thenReturn(java.util.Optional.of(entity));

            registry.unregisterPlugin(PROJECT_ID, "to-remove");

            verify(registeredPluginRepository).delete(entity);
        }

        @Test
        void unregisterThrowsForUnknown() {
            when(registeredPluginRepository.findByProjectIdAndName(PROJECT_ID, "nonexistent"))
                    .thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> registry.unregisterPlugin(PROJECT_ID, "nonexistent"))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
