package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.packregistry.service.CreateTrustPolicyCommand;
import com.keplerops.groundcontrol.domain.packregistry.service.TrustPolicyService;
import com.keplerops.groundcontrol.domain.packregistry.service.UpdateTrustPolicyCommand;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/trust-policies")
public class TrustPolicyController {

    private final TrustPolicyService trustPolicyService;
    private final ProjectService projectService;
    private final PackRegistryAccessGuard accessGuard;

    public TrustPolicyController(
            TrustPolicyService trustPolicyService, ProjectService projectService, PackRegistryAccessGuard accessGuard) {
        this.trustPolicyService = trustPolicyService;
        this.projectService = projectService;
        this.accessGuard = accessGuard;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrustPolicyResponse create(
            @Valid @RequestBody CreateTrustPolicyRequest request,
            @RequestParam(required = false) String project,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.resolveProjectId(project);
        var result = trustPolicyService.create(new CreateTrustPolicyCommand(
                projectId,
                request.name(),
                request.description(),
                request.defaultOutcome(),
                TrustPolicyRuleRequest.toDomainList(request.rules()),
                request.priority(),
                request.enabled()));
        return TrustPolicyResponse.from(result);
    }

    @GetMapping
    public List<TrustPolicyResponse> list(
            @RequestParam(required = false) String project, HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var projectId = projectService.resolveProjectId(project);
        return trustPolicyService.list(projectId).stream()
                .map(TrustPolicyResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TrustPolicyResponse get(@PathVariable UUID id, HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        return TrustPolicyResponse.from(trustPolicyService.get(id));
    }

    @PutMapping("/{id}")
    public TrustPolicyResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTrustPolicyRequest request,
            HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        var result = trustPolicyService.update(
                id,
                new UpdateTrustPolicyCommand(
                        request.name(),
                        request.description(),
                        request.defaultOutcome(),
                        TrustPolicyRuleRequest.toDomainList(request.rules()),
                        request.priority(),
                        request.enabled()));
        return TrustPolicyResponse.from(result);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, HttpServletRequest httpRequest) {
        accessGuard.requireAdminActor(httpRequest);
        trustPolicyService.delete(id);
    }
}
