package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.api.controlpacks.ControlPackEntryDefinitionRequest;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition;
import com.keplerops.groundcontrol.domain.packregistry.service.InstallPackCommand;
import com.keplerops.groundcontrol.domain.packregistry.service.PackInstallOrchestrator;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pack-install-records")
public class PackInstallRecordController {

    private final PackInstallOrchestrator orchestrator;
    private final ProjectService projectService;

    public PackInstallRecordController(PackInstallOrchestrator orchestrator, ProjectService projectService) {
        this.orchestrator = orchestrator;
        this.projectService = projectService;
    }

    @PostMapping("/install")
    @ResponseStatus(HttpStatus.CREATED)
    public PackInstallRecordResponse install(
            @Valid @RequestBody InstallPackRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var result = orchestrator.installPack(new InstallPackCommand(
                projectId,
                request.packId(),
                request.versionConstraint(),
                request.performedBy(),
                request.entries().stream()
                        .map(PackInstallRecordController::toEntryDefinition)
                        .toList()));
        return PackInstallRecordResponse.from(result);
    }

    @PostMapping("/upgrade")
    public PackInstallRecordResponse upgrade(
            @Valid @RequestBody InstallPackRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var result = orchestrator.upgradePack(new InstallPackCommand(
                projectId,
                request.packId(),
                request.versionConstraint(),
                request.performedBy(),
                request.entries().stream()
                        .map(PackInstallRecordController::toEntryDefinition)
                        .toList()));
        return PackInstallRecordResponse.from(result);
    }

    @GetMapping
    public List<PackInstallRecordResponse> list(
            @RequestParam(required = false) String project, @RequestParam(required = false) String packId) {
        var projectId = projectService.resolveProjectId(project);
        var records = packId != null
                ? orchestrator.listInstallRecords(projectId, packId)
                : orchestrator.listInstallRecords(projectId);
        return records.stream().map(PackInstallRecordResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PackInstallRecordResponse get(@PathVariable UUID id) {
        return PackInstallRecordResponse.from(orchestrator.getInstallRecord(id));
    }

    private static ControlPackEntryDefinition toEntryDefinition(ControlPackEntryDefinitionRequest r) {
        return new ControlPackEntryDefinition(
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
