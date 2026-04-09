package com.keplerops.groundcontrol.domain.plugins.service;

import com.keplerops.groundcontrol.domain.exception.ConflictException;
import com.keplerops.groundcontrol.domain.exception.NotFoundException;
import com.keplerops.groundcontrol.domain.plugins.model.RegisteredPlugin;
import com.keplerops.groundcontrol.domain.plugins.repository.RegisteredPluginRepository;
import com.keplerops.groundcontrol.domain.plugins.state.PluginLifecycleState;
import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PluginRegistry {

    private static final Logger log = LoggerFactory.getLogger(PluginRegistry.class);

    private final List<Plugin> classpathPlugins;
    private final RegisteredPluginRepository registeredPluginRepository;
    private final ProjectService projectService;
    private final Map<String, ManagedPlugin> registry = new ConcurrentHashMap<>();

    public PluginRegistry(
            List<Plugin> classpathPlugins,
            RegisteredPluginRepository registeredPluginRepository,
            ProjectService projectService) {
        this.classpathPlugins = classpathPlugins;
        this.registeredPluginRepository = registeredPluginRepository;
        this.projectService = projectService;
    }

    @PostConstruct
    public void initializeClasspathPlugins() {
        for (Plugin plugin : classpathPlugins) {
            var descriptor = plugin.descriptor();
            if (registry.containsKey(descriptor.name())) {
                log.warn("plugin_duplicate_skipped: name={}", descriptor.name());
                continue;
            }
            var managed = new ManagedPlugin(plugin, PluginLifecycleState.CREATED, true);
            managed = runLifecycle(managed);
            registry.put(descriptor.name(), managed);
        }
        log.info("plugin_registry_initialized: builtin_count={}", registry.size());
    }

    @Transactional(readOnly = true)
    public List<PluginInfo> listPlugins() {
        var results = new java.util.ArrayList<>(
                registry.values().stream().map(ManagedPlugin::toInfo).toList());
        results.addAll(listDynamicPlugins());
        return Collections.unmodifiableList(results);
    }

    @Transactional(readOnly = true)
    public List<PluginInfo> listPlugins(UUID projectId) {
        var results = new java.util.ArrayList<>(
                registry.values().stream().map(ManagedPlugin::toInfo).toList());
        results.addAll(listDynamicPluginsByProject(projectId));
        return Collections.unmodifiableList(results);
    }

    @Transactional(readOnly = true)
    public PluginInfo getPlugin(String name) {
        var managed = registry.get(name);
        if (managed != null) {
            return managed.toInfo();
        }
        var dynamic = registeredPluginRepository.findAll().stream()
                .filter(rp -> rp.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Plugin not found: " + name));
        return toDynamicInfo(dynamic);
    }

    @Transactional(readOnly = true)
    public List<PluginInfo> listByType(PluginType type) {
        var results = new java.util.ArrayList<>(registry.values().stream()
                .filter(mp -> mp.descriptor().type() == type)
                .map(ManagedPlugin::toInfo)
                .toList());
        registeredPluginRepository.findAll().stream()
                .filter(rp -> rp.getPluginType() == type)
                .map(this::toDynamicInfo)
                .forEach(results::add);
        return Collections.unmodifiableList(results);
    }

    @Transactional(readOnly = true)
    public List<PluginInfo> listByCapability(String capability) {
        var results = new java.util.ArrayList<>(registry.values().stream()
                .filter(mp -> mp.descriptor().capabilities() != null
                        && mp.descriptor().capabilities().contains(capability))
                .map(ManagedPlugin::toInfo)
                .toList());
        registeredPluginRepository.findAll().stream()
                .filter(rp ->
                        rp.getCapabilities() != null && rp.getCapabilities().contains(capability))
                .map(this::toDynamicInfo)
                .forEach(results::add);
        return Collections.unmodifiableList(results);
    }

    public PluginInfo registerPlugin(RegisterPluginCommand command) {
        var project = projectService.getById(command.projectId());

        if (registry.containsKey(command.name())) {
            throw new ConflictException("Plugin name conflicts with built-in plugin: " + command.name());
        }
        if (registeredPluginRepository.existsByProjectIdAndName(command.projectId(), command.name())) {
            throw new ConflictException("Plugin already registered in project: " + command.name());
        }

        var entity = new RegisteredPlugin(project, command.name(), command.version(), command.type());
        entity.setDescription(command.description());
        entity.setCapabilities(command.capabilities());
        entity.setMetadata(command.metadata());
        entity.setLifecycleState(PluginLifecycleState.STARTED);

        entity = registeredPluginRepository.save(entity);
        log.info(
                "plugin_registered: name={} type={} project={}",
                entity.getName(),
                entity.getPluginType(),
                project.getIdentifier());

        return toDynamicInfo(entity);
    }

    public void unregisterPlugin(UUID projectId, String name) {
        var entity = registeredPluginRepository
                .findByProjectIdAndName(projectId, name)
                .orElseThrow(() -> new NotFoundException("Dynamic plugin not found: " + name));

        registeredPluginRepository.delete(entity);
        log.info("plugin_unregistered: name={} projectId={}", name, projectId);
    }

    private ManagedPlugin runLifecycle(ManagedPlugin managed) {
        try {
            managed.plugin().initialize();
            managed = managed.withState(PluginLifecycleState.INITIALIZED);
            managed.plugin().start();
            managed = managed.withState(PluginLifecycleState.STARTED);
            log.info(
                    "plugin_started: name={} type={}",
                    managed.descriptor().name(),
                    managed.descriptor().type());
        } catch (Exception e) {
            managed = managed.withState(PluginLifecycleState.FAILED);
            log.error("plugin_lifecycle_failed: name={}", managed.descriptor().name(), e);
        }
        return managed;
    }

    private List<PluginInfo> listDynamicPlugins() {
        return registeredPluginRepository.findAll().stream()
                .map(this::toDynamicInfo)
                .toList();
    }

    private List<PluginInfo> listDynamicPluginsByProject(UUID projectId) {
        return registeredPluginRepository.findByProjectId(projectId).stream()
                .map(this::toDynamicInfo)
                .toList();
    }

    private PluginInfo toDynamicInfo(RegisteredPlugin entity) {
        return new PluginInfo(
                entity.getName(),
                entity.getVersion(),
                entity.getDescription(),
                entity.getPluginType(),
                entity.getCapabilities() != null ? entity.getCapabilities() : Set.of(),
                entity.getMetadata() != null ? entity.getMetadata() : Map.of(),
                entity.getLifecycleState(),
                entity.isEnabled(),
                false);
    }

    private record ManagedPlugin(Plugin plugin, PluginLifecycleState state, boolean builtin) {

        PluginDescriptor descriptor() {
            return plugin.descriptor();
        }

        ManagedPlugin withState(PluginLifecycleState newState) {
            return new ManagedPlugin(plugin, newState, builtin);
        }

        PluginInfo toInfo() {
            var desc = plugin.descriptor();
            return new PluginInfo(
                    desc.name(),
                    desc.version(),
                    desc.description(),
                    desc.type(),
                    desc.capabilities() != null ? desc.capabilities() : Set.of(),
                    desc.metadata() != null ? desc.metadata() : Map.of(),
                    state,
                    plugin.isAvailable(),
                    builtin);
        }
    }
}
