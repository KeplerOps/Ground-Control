package com.keplerops.groundcontrol.api.audits;

import com.keplerops.groundcontrol.domain.audits.service.AuditLinkService;
import com.keplerops.groundcontrol.domain.audits.service.CreateAuditLinkCommand;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/v1/audits/{auditId}/links")
public class AuditLinkController {

    private final AuditLinkService linkService;
    private final ProjectService projectService;

    public AuditLinkController(AuditLinkService linkService, ProjectService projectService) {
        this.linkService = linkService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditLinkResponse create(
            @PathVariable UUID auditId,
            @Valid @RequestBody AuditLinkRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateAuditLinkCommand(
                request.targetType(),
                request.targetEntityId(),
                request.targetIdentifier(),
                request.linkType(),
                request.targetUrl(),
                request.targetTitle());
        return AuditLinkResponse.from(linkService.create(projectId, auditId, command), auditId);
    }

    @GetMapping
    public List<AuditLinkResponse> list(@PathVariable UUID auditId, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return linkService.listByAudit(projectId, auditId).stream()
                .map(link -> AuditLinkResponse.from(link, auditId))
                .toList();
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID auditId, @PathVariable UUID linkId, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        linkService.delete(projectId, auditId, linkId);
    }
}
