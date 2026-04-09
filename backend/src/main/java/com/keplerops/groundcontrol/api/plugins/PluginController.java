package com.keplerops.groundcontrol.api.plugins;

import com.keplerops.groundcontrol.domain.plugins.service.PluginRegistry;
import com.keplerops.groundcontrol.domain.plugins.service.RegisterPluginCommand;
import com.keplerops.groundcontrol.domain.plugins.state.PluginType;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plugins")
public class PluginController {

    private final PluginRegistry pluginRegistry;
    private final ProjectService projectService;

    public PluginController(PluginRegistry pluginRegistry, ProjectService projectService) {
        this.pluginRegistry = pluginRegistry;
        this.projectService = projectService;
    }

    @GetMapping
    public List<PluginResponse> list(
            @RequestParam(required = false) String project,
            @RequestParam(required = false) PluginType type,
            @RequestParam(required = false) String capability) {
        if (type != null) {
            return pluginRegistry.listByType(type).stream()
                    .map(PluginResponse::from)
                    .toList();
        }
        if (capability != null) {
            return pluginRegistry.listByCapability(capability).stream()
                    .map(PluginResponse::from)
                    .toList();
        }
        if (project != null) {
            var projectId = projectService.resolveProjectId(project);
            return pluginRegistry.listPlugins(projectId).stream()
                    .map(PluginResponse::from)
                    .toList();
        }
        return pluginRegistry.listPlugins().stream().map(PluginResponse::from).toList();
    }

    @GetMapping("/{name}")
    public PluginResponse getByName(@PathVariable String name) {
        return PluginResponse.from(pluginRegistry.getPlugin(name));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PluginResponse register(
            @Valid @RequestBody RegisterPluginRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return PluginResponse.from(pluginRegistry.registerPlugin(new RegisterPluginCommand(
                projectId,
                request.name(),
                request.version(),
                request.description(),
                request.type(),
                request.capabilities(),
                request.metadata())));
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@PathVariable String name, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        pluginRegistry.unregisterPlugin(projectId, name);
    }
}
