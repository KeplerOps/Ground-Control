package com.keplerops.groundcontrol.api.assets;

import com.keplerops.groundcontrol.domain.assets.service.AssetService;
import com.keplerops.groundcontrol.domain.assets.service.AssetTopologyService;
import com.keplerops.groundcontrol.domain.assets.service.CreateAssetCommand;
import com.keplerops.groundcontrol.domain.assets.service.CreateAssetLinkCommand;
import com.keplerops.groundcontrol.domain.assets.service.UpdateAssetCommand;
import com.keplerops.groundcontrol.domain.assets.state.AssetLinkTargetType;
import com.keplerops.groundcontrol.domain.assets.state.AssetType;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;
    private final AssetTopologyService topologyService;
    private final ProjectService projectService;

    public AssetController(
            AssetService assetService, AssetTopologyService topologyService, ProjectService projectService) {
        this.assetService = assetService;
        this.topologyService = topologyService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse create(
            @Valid @RequestBody AssetRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateAssetCommand(
                projectId, request.uid(), request.name(), request.description(), request.assetType());
        return AssetResponse.from(assetService.create(command));
    }

    @GetMapping
    public List<AssetResponse> list(
            @RequestParam(required = false) String project, @RequestParam(required = false) AssetType type) {
        var projectId = projectService.resolveProjectId(project);
        if (type != null) {
            return assetService.listByProjectAndType(projectId, type).stream()
                    .map(AssetResponse::from)
                    .toList();
        }
        return assetService.listByProject(projectId).stream()
                .map(AssetResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AssetResponse getById(@PathVariable UUID id) {
        return AssetResponse.from(assetService.getById(id));
    }

    @GetMapping("/uid/{uid}")
    public AssetResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return AssetResponse.from(assetService.getByUid(projectId, uid));
    }

    @PutMapping("/{id}")
    public AssetResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAssetRequest request) {
        var command = new UpdateAssetCommand(request.name(), request.description(), request.assetType());
        return AssetResponse.from(assetService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        assetService.delete(id);
    }

    @PostMapping("/{id}/archive")
    public AssetResponse archive(@PathVariable UUID id) {
        return AssetResponse.from(assetService.archive(id));
    }

    @PostMapping("/{id}/relations")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetRelationResponse createRelation(
            @PathVariable UUID id, @Valid @RequestBody AssetRelationRequest request) {
        return AssetRelationResponse.from(assetService.createRelation(id, request.targetId(), request.relationType()));
    }

    @GetMapping("/{id}/relations")
    public List<AssetRelationResponse> getRelations(@PathVariable UUID id) {
        return assetService.getRelations(id).stream()
                .map(AssetRelationResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}/relations/{relationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRelation(@PathVariable UUID id, @PathVariable UUID relationId) {
        assetService.deleteRelation(id, relationId);
    }

    // --- Asset Links (cross-entity linking) ---

    @PostMapping("/{id}/links")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetLinkResponse createLink(@PathVariable UUID id, @Valid @RequestBody AssetLinkRequest request) {
        var command = new CreateAssetLinkCommand(
                request.targetType(),
                request.targetIdentifier(),
                request.linkType(),
                request.targetUrl(),
                request.targetTitle());
        return AssetLinkResponse.from(assetService.createLink(id, command));
    }

    @GetMapping("/{id}/links")
    public List<AssetLinkResponse> getLinks(
            @PathVariable UUID id,
            @RequestParam(name = "target_type", required = false) AssetLinkTargetType targetType) {
        if (targetType != null) {
            return assetService.getLinksForAssetByTargetType(id, targetType).stream()
                    .map(AssetLinkResponse::from)
                    .toList();
        }
        return assetService.getLinksForAsset(id).stream()
                .map(AssetLinkResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}/links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLink(@PathVariable UUID id, @PathVariable UUID linkId) {
        assetService.deleteLink(id, linkId);
    }

    @GetMapping("/links/by-target")
    public List<AssetLinkResponse> getLinksByTarget(
            @RequestParam("target_type") AssetLinkTargetType targetType,
            @RequestParam("target_identifier") String targetIdentifier,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return assetService.getLinksByTarget(projectId, targetType, targetIdentifier).stream()
                .map(AssetLinkResponse::from)
                .toList();
    }

    // --- Topology ---

    @GetMapping("/topology/cycles")
    public List<AssetCycleResponse> detectCycles(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return topologyService.detectCycles(projectId).stream()
                .map(AssetCycleResponse::from)
                .toList();
    }

    @GetMapping("/{id}/topology/impact")
    public List<AssetResponse> impactAnalysis(@PathVariable UUID id) {
        return topologyService.impactAnalysis(id).stream()
                .map(AssetResponse::from)
                .toList();
    }

    @PostMapping("/topology/subgraph")
    public AssetSubgraphResponse extractSubgraph(
            @Valid @RequestBody SubgraphRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return AssetSubgraphResponse.from(topologyService.extractSubgraph(projectId, request.rootUids()));
    }
}
