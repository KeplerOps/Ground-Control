package com.keplerops.groundcontrol.api.audits;

import com.keplerops.groundcontrol.domain.audits.service.AuditService;
import com.keplerops.groundcontrol.domain.audits.service.CreateAuditCommand;
import com.keplerops.groundcontrol.domain.audits.service.UpdateAuditCommand;
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

@RestController("auditsAggregateController")
@RequestMapping("/api/v1/audits")
public class AuditController {

    private final AuditService auditService;
    private final ProjectService projectService;

    public AuditController(AuditService auditService, ProjectService projectService) {
        this.auditService = auditService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditResponse create(
            @Valid @RequestBody AuditRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateAuditCommand(
                projectId,
                request.uid(),
                request.title(),
                request.auditType(),
                request.scopeDescription(),
                request.objectives(),
                request.phases() == null
                        ? null
                        : request.phases().stream().map(AuditPhaseDto::toDomain).toList(),
                request.teamMembers());
        return AuditResponse.from(auditService.create(command));
    }

    @GetMapping
    public List<AuditResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return auditService.listByProject(projectId).stream()
                .map(AuditResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AuditResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return AuditResponse.from(auditService.getById(projectId, id));
    }

    @GetMapping("/uid/{uid}")
    public AuditResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return AuditResponse.from(auditService.getByUid(uid, projectId));
    }

    @PutMapping("/{id}")
    public AuditResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAuditRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new UpdateAuditCommand(
                request.title(),
                request.auditType(),
                request.scopeDescription(),
                request.objectives(),
                request.phases() == null
                        ? null
                        : request.phases().stream().map(AuditPhaseDto::toDomain).toList(),
                request.teamMembers(),
                Boolean.TRUE.equals(request.clearObjectives()),
                Boolean.TRUE.equals(request.clearPhases()),
                Boolean.TRUE.equals(request.clearTeamMembers()));
        return AuditResponse.from(auditService.update(projectId, id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        auditService.delete(projectId, id);
    }

    @PutMapping("/{id}/status")
    public AuditResponse transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AuditStatusTransitionRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return AuditResponse.from(auditService.transitionStatus(projectId, id, request.status()));
    }
}
