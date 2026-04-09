package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.packregistry.service.InstallPackCommand;
import com.keplerops.groundcontrol.domain.packregistry.service.PackInstallOrchestrator;
import com.keplerops.groundcontrol.domain.packregistry.state.InstallOutcome;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pack-install-records")
public class PackInstallRecordController {

    private final PackInstallOrchestrator orchestrator;
    private final ProjectService projectService;
    private final PackRegistryAccessGuard accessGuard;

    public PackInstallRecordController(
            PackInstallOrchestrator orchestrator, ProjectService projectService, PackRegistryAccessGuard accessGuard) {
        this.orchestrator = orchestrator;
        this.projectService = projectService;
        this.accessGuard = accessGuard;
    }

    @PostMapping("/install")
    public ResponseEntity<PackInstallRecordResponse> install(
            @Valid @RequestBody InstallPackRequest request,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        var performedBy = accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.resolveProjectId(project);
        var result = orchestrator.installPack(
                new InstallPackCommand(projectId, request.packId(), request.versionConstraint(), performedBy));
        var response = PackInstallRecordResponse.from(result);
        return ResponseEntity.status(statusForOutcome(response.installOutcome(), HttpStatus.CREATED))
                .body(response);
    }

    @PostMapping("/upgrade")
    public ResponseEntity<PackInstallRecordResponse> upgrade(
            @Valid @RequestBody InstallPackRequest request,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        var performedBy = accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.resolveProjectId(project);
        var result = orchestrator.upgradePack(
                new InstallPackCommand(projectId, request.packId(), request.versionConstraint(), performedBy));
        var response = PackInstallRecordResponse.from(result);
        return ResponseEntity.status(statusForOutcome(response.installOutcome(), HttpStatus.OK))
                .body(response);
    }

    @GetMapping
    public List<PackInstallRecordResponse> list(
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String packId,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.resolveProjectId(project);
        var records = packId != null
                ? orchestrator.listInstallRecords(projectId, packId)
                : orchestrator.listInstallRecords(projectId);
        return records.stream().map(PackInstallRecordResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PackInstallRecordResponse get(@PathVariable UUID id, HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        return PackInstallRecordResponse.from(orchestrator.getInstallRecord(id));
    }

    private HttpStatus statusForOutcome(InstallOutcome outcome, HttpStatus successStatus) {
        return switch (outcome) {
            case INSTALLED, UPGRADED -> successStatus;
            case REJECTED, FAILED -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
}
