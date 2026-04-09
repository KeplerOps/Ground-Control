package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.api.controlpacks.ControlPackEntryDefinitionRequest;
import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition;
import com.keplerops.groundcontrol.domain.packregistry.service.ControlPackRegistrationContent;
import com.keplerops.groundcontrol.domain.packregistry.service.EmptyPackRegistrationContent;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistrationContent;
import com.keplerops.groundcontrol.domain.packregistry.service.PackRegistryService;
import com.keplerops.groundcontrol.domain.packregistry.service.PackResolver;
import com.keplerops.groundcontrol.domain.packregistry.service.RegisterPackCommand;
import com.keplerops.groundcontrol.domain.packregistry.service.UpdatePackRegistryEntryCommand;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/pack-registry")
public class PackRegistryController {

    private final PackRegistryService registryService;
    private final PackResolver packResolver;
    private final ProjectService projectService;
    private final PackRegistryAccessGuard accessGuard;

    public PackRegistryController(
            PackRegistryService registryService,
            PackResolver packResolver,
            ProjectService projectService,
            PackRegistryAccessGuard accessGuard) {
        this.registryService = registryService;
        this.packResolver = packResolver;
        this.projectService = projectService;
        this.accessGuard = accessGuard;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PackRegistryEntryResponse register(
            @Valid @RequestBody RegisterPackRequest request,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.resolveProjectId(project);
        var result = registryService.registerEntry(new RegisterPackCommand(
                projectId,
                request.packId(),
                request.packType(),
                request.version(),
                request.publisher(),
                request.description(),
                request.sourceUrl(),
                request.checksum(),
                request.signatureInfo(),
                request.compatibility(),
                PackDependencyRequest.toDomainList(request.dependencies()),
                toRegistrationContent(request.controlPackEntries()),
                request.provenance(),
                request.registryMetadata()));
        return PackRegistryEntryResponse.from(result);
    }

    @GetMapping
    public List<PackRegistryEntryResponse> list(
            @RequestParam(required = false) String project,
            @RequestParam(required = false) PackType packType,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.resolveProjectId(project);
        var entries = packType != null
                ? registryService.listEntries(projectId, packType)
                : registryService.listEntries(projectId);
        return entries.stream().map(PackRegistryEntryResponse::from).toList();
    }

    @GetMapping("/{packId}")
    public List<PackRegistryEntryResponse> listVersions(
            @PathVariable String packId,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.requireProjectId(project);
        return registryService.listVersions(projectId, packId).stream()
                .map(PackRegistryEntryResponse::from)
                .toList();
    }

    @GetMapping("/{packId}/{version}")
    public PackRegistryEntryResponse getEntry(
            @PathVariable String packId,
            @PathVariable String version,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.requireProjectId(project);
        return PackRegistryEntryResponse.from(registryService.findEntry(projectId, packId, version));
    }

    @PutMapping("/{packId}/{version}")
    public PackRegistryEntryResponse updateEntry(
            @PathVariable String packId,
            @PathVariable String version,
            @Valid @RequestBody UpdatePackRegistryEntryRequest request,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.requireProjectId(project);
        var result = registryService.updateEntry(
                projectId,
                packId,
                version,
                new UpdatePackRegistryEntryCommand(
                        request.publisher(),
                        request.description(),
                        request.sourceUrl(),
                        request.checksum(),
                        request.signatureInfo(),
                        request.compatibility(),
                        PackDependencyRequest.toDomainList(request.dependencies()),
                        request.controlPackEntries() != null
                                ? toRegistrationContent(request.controlPackEntries())
                                : null,
                        request.provenance(),
                        request.registryMetadata()));
        return PackRegistryEntryResponse.from(result);
    }

    @PutMapping("/{packId}/{version}/withdraw")
    public PackRegistryEntryResponse withdraw(
            @PathVariable String packId,
            @PathVariable String version,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.requireProjectId(project);
        return PackRegistryEntryResponse.from(registryService.withdrawEntry(projectId, packId, version));
    }

    @DeleteMapping("/{packId}/{version}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(
            @PathVariable String packId,
            @PathVariable String version,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.requireProjectId(project);
        registryService.deleteEntry(projectId, packId, version);
    }

    @PostMapping("/resolve")
    public ResolvedPackResponse resolve(
            @Valid @RequestBody ResolvePackRequest request,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.requireProjectId(project);
        var resolved = packResolver.resolve(projectId, request.packId(), request.versionConstraint());
        var compatible = packResolver.checkCompatibility(resolved);
        return ResolvedPackResponse.from(resolved, compatible, packResolver);
    }

    @PostMapping("/check-compatibility")
    public CompatibilityCheckResponse checkCompatibility(
            @Valid @RequestBody ResolvePackRequest request,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.requireProjectId(project);
        var resolved = packResolver.resolve(projectId, request.packId(), request.versionConstraint());
        var compatible = packResolver.checkCompatibility(resolved);
        return new CompatibilityCheckResponse(request.packId(), resolved.resolvedVersion(), compatible);
    }

    private static ControlPackEntryDefinition toControlPackEntryDefinition(ControlPackEntryDefinitionRequest request) {
        return new ControlPackEntryDefinition(
                request.uid(),
                request.title(),
                request.description(),
                request.objective(),
                request.controlFunction(),
                request.owner(),
                request.implementationScope(),
                request.methodologyFactors(),
                request.effectiveness(),
                request.category(),
                request.source(),
                request.implementationGuidance(),
                request.expectedEvidence(),
                request.frameworkMappings());
    }

    private static PackRegistrationContent toRegistrationContent(List<ControlPackEntryDefinitionRequest> requests) {
        if (requests == null) {
            return EmptyPackRegistrationContent.INSTANCE;
        }
        return new ControlPackRegistrationContent(requests.stream()
                .map(PackRegistryController::toControlPackEntryDefinition)
                .toList());
    }
}
