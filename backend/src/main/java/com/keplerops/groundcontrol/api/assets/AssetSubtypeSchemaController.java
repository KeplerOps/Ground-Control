package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.service.AssetService;
import com.keplerops.groundcontrol.domain.assets.service.CreateAssetSubtypeSchemaCommand;
import com.keplerops.groundcontrol.domain.assets.service.UpdateAssetSubtypeSchemaCommand;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets/subtype-schemas")
public class AssetSubtypeSchemaController {

    private final AssetService assetService;
    private final ProjectService projectService;

    public AssetSubtypeSchemaController(AssetService assetService, ProjectService projectService) {
        this.assetService = assetService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetSubtypeSchemaResponse register(
            @Valid @RequestBody AssetSubtypeSchemaRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateAssetSubtypeSchemaCommand(
                projectId,
                request.assetType(),
                request.subtype(),
                request.schemaVersion(),
                request.description(),
                request.schemaBody());
        return AssetSubtypeSchemaResponse.from(assetService.registerSubtypeSchema(command));
    }

    @GetMapping
    public List<AssetSubtypeSchemaResponse> list(
            @RequestParam(required = false) String project,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) String subtype) {
        var projectId = projectService.resolveProjectId(project);
        return assetService.listSubtypeSchemas(projectId, assetType, subtype).stream()
                .map(AssetSubtypeSchemaResponse::from)
                .toList();
    }

    @GetMapping("/active")
    public AssetSubtypeSchemaResponse getActive(
            @RequestParam(required = false) String project,
            @RequestParam AssetType assetType,
            @RequestParam String subtype) {
        var projectId = projectService.requireProjectId(project);
        return AssetSubtypeSchemaResponse.from(assetService.getActiveSubtypeSchema(projectId, assetType, subtype));
    }

    @GetMapping("/{id}")
    public AssetSubtypeSchemaResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return AssetSubtypeSchemaResponse.from(assetService.getSubtypeSchema(projectId, id));
    }

    @PutMapping("/{id}")
    public AssetSubtypeSchemaResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssetSubtypeSchemaRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        var command = new UpdateAssetSubtypeSchemaCommand(
                request.description(), request.schemaBody(), request.clearDescription(), request.clearSchemaBody());
        return AssetSubtypeSchemaResponse.from(assetService.updateSubtypeSchema(projectId, id, command));
    }

    @PostMapping("/{id}/deprecate")
    public AssetSubtypeSchemaResponse deprecate(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return AssetSubtypeSchemaResponse.from(assetService.deprecateSubtypeSchema(projectId, id));
    }
}
