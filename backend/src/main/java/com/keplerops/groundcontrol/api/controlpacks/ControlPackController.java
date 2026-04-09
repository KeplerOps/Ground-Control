package com.keplerops.groundcontrol.api.controlpacks;

import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackService;
import com.keplerops.groundcontrol.domain.controlpacks.service.CreateControlPackOverrideCommand;
import com.keplerops.groundcontrol.domain.controlpacks.service.InstallControlPackCommand;
import com.keplerops.groundcontrol.domain.controlpacks.service.UpgradeControlPackCommand;
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
@RequestMapping("/api/v1/control-packs")
public class ControlPackController {

    private final ControlPackService controlPackService;
    private final ProjectService projectService;

    public ControlPackController(ControlPackService controlPackService, ProjectService projectService) {
        this.controlPackService = controlPackService;
        this.projectService = projectService;
    }

    @PostMapping("/install")
    @ResponseStatus(HttpStatus.CREATED)
    public ControlPackInstallResultResponse install(
            @Valid @RequestBody InstallControlPackRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var result = controlPackService.install(new InstallControlPackCommand(
                projectId,
                request.packId(),
                request.version(),
                request.publisher(),
                request.description(),
                request.sourceUrl(),
                request.checksum(),
                request.compatibility(),
                request.packMetadata(),
                request.entries().stream()
                        .map(ControlPackController::toEntryDefinition)
                        .toList()));
        return ControlPackInstallResultResponse.from(result);
    }

    @PostMapping("/upgrade")
    public ControlPackUpgradeResultResponse upgrade(
            @Valid @RequestBody UpgradeControlPackRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var result = controlPackService.upgrade(new UpgradeControlPackCommand(
                projectId,
                request.packId(),
                request.newVersion(),
                request.publisher(),
                request.description(),
                request.sourceUrl(),
                request.checksum(),
                request.compatibility(),
                request.packMetadata(),
                request.entries().stream()
                        .map(ControlPackController::toEntryDefinition)
                        .toList()));
        return ControlPackUpgradeResultResponse.from(result);
    }

    @GetMapping
    public List<ControlPackResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return controlPackService.listByProject(projectId).stream()
                .map(ControlPackResponse::from)
                .toList();
    }

    @GetMapping("/{packId}")
    public ControlPackResponse getByPackId(
            @PathVariable String packId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlPackResponse.from(controlPackService.getByPackId(projectId, packId));
    }

    @PutMapping("/{packId}/deprecate")
    public ControlPackResponse deprecate(@PathVariable String packId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlPackResponse.from(controlPackService.deprecate(projectId, packId));
    }

    @DeleteMapping("/{packId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String packId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        controlPackService.remove(projectId, packId);
    }

    @GetMapping("/{packId}/entries")
    public List<ControlPackEntryResponse> listEntries(
            @PathVariable String packId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return controlPackService.listEntries(projectId, packId).stream()
                .map(ControlPackEntryResponse::from)
                .toList();
    }

    @GetMapping("/{packId}/entries/{entryUid}")
    public ControlPackEntryResponse getEntry(
            @PathVariable String packId,
            @PathVariable String entryUid,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlPackEntryResponse.from(controlPackService.getEntry(projectId, packId, entryUid));
    }

    @PostMapping("/{packId}/entries/{entryUid}/overrides")
    @ResponseStatus(HttpStatus.CREATED)
    public ControlPackOverrideResponse createOverride(
            @PathVariable String packId,
            @PathVariable String entryUid,
            @Valid @RequestBody CreateControlPackOverrideRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlPackOverrideResponse.from(controlPackService.createOverride(
                projectId,
                packId,
                entryUid,
                new CreateControlPackOverrideCommand(request.fieldName(), request.overrideValue(), request.reason())));
    }

    @GetMapping("/{packId}/entries/{entryUid}/overrides")
    public List<ControlPackOverrideResponse> listOverrides(
            @PathVariable String packId,
            @PathVariable String entryUid,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return controlPackService.listOverrides(projectId, packId, entryUid).stream()
                .map(ControlPackOverrideResponse::from)
                .toList();
    }

    @DeleteMapping("/{packId}/entries/{entryUid}/overrides/{overrideId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOverride(
            @PathVariable String packId,
            @PathVariable String entryUid,
            @PathVariable UUID overrideId,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        controlPackService.deleteOverride(projectId, packId, entryUid, overrideId);
    }

    private static com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition toEntryDefinition(
            ControlPackEntryDefinitionRequest r) {
        return new com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition(
                r.uid(),
                r.title(),
                r.description(),
                r.objective(),
                r.controlFunction(),
                r.owner(),
                r.implementationScope(),
                r.methodologyFactors(),
                r.effectiveness(),
                r.category(),
                r.source(),
                r.implementationGuidance(),
                r.expectedEvidence(),
                r.frameworkMappings());
    }
}
