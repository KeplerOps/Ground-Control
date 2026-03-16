package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.AuditService;
import com.keplerops.groundcontrol.domain.requirements.service.CloneRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.service.CreateRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.service.CreateTraceabilityLinkCommand;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementFilter;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementService;
import com.keplerops.groundcontrol.domain.requirements.service.TraceabilityService;
import com.keplerops.groundcontrol.domain.requirements.service.UpdateRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.state.Priority;
import com.keplerops.groundcontrol.domain.requirements.state.RequirementType;
import com.keplerops.groundcontrol.domain.requirements.state.Status;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1/requirements")
public class RequirementController {

    private final RequirementService requirementService;
    private final TraceabilityService traceabilityService;
    private final AuditService auditService;
    private final ProjectService projectService;

    public RequirementController(
            RequirementService requirementService,
            TraceabilityService traceabilityService,
            AuditService auditService,
            ProjectService projectService) {
        this.requirementService = requirementService;
        this.traceabilityService = traceabilityService;
        this.auditService = auditService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementResponse create(
            @Valid @RequestBody RequirementRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateRequirementCommand(
                projectId,
                request.uid(),
                request.title(),
                request.statement(),
                request.rationale(),
                request.requirementType(),
                request.priority(),
                request.wave());
        return RequirementResponse.from(requirementService.create(command));
    }

    @GetMapping
    public Page<RequirementResponse> list(
            Pageable pageable,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) RequirementType type,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Integer wave,
            @RequestParam(required = false) String search) {
        var projectId = projectService.resolveProjectId(project);
        var filter = new RequirementFilter(status, type, priority, wave, search);
        return requirementService.list(projectId, pageable, filter).map(RequirementResponse::from);
    }

    @GetMapping("/{id}")
    public RequirementResponse getById(@PathVariable UUID id) {
        return RequirementResponse.from(requirementService.getById(id));
    }

    @GetMapping("/uid/{uid}")
    public RequirementResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return RequirementResponse.from(requirementService.getByUid(projectId, uid));
    }

    @PutMapping("/{id}")
    public RequirementResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRequirementRequest request) {
        var command = new UpdateRequirementCommand(
                request.title(),
                request.statement(),
                request.rationale(),
                request.requirementType(),
                request.priority(),
                request.wave());
        return RequirementResponse.from(requirementService.update(id, command));
    }

    @PostMapping("/{id}/transition")
    public RequirementResponse transitionStatus(
            @PathVariable UUID id, @Valid @RequestBody StatusTransitionRequest request) {
        return RequirementResponse.from(requirementService.transitionStatus(id, request.status()));
    }

    @PostMapping("/bulk/transition")
    public BulkStatusTransitionResponse bulkTransitionStatus(@Valid @RequestBody BulkStatusTransitionRequest request) {
        var result = requirementService.bulkTransitionStatus(request.ids(), request.status());
        return BulkStatusTransitionResponse.from(result, request.ids().size());
    }

    @PostMapping("/{id}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementResponse clone(@PathVariable UUID id, @Valid @RequestBody CloneRequirementRequest request) {
        var command = new CloneRequirementCommand(request.newUid(), request.copyRelations());
        return RequirementResponse.from(requirementService.clone(id, command));
    }

    @GetMapping("/{id}/history")
    public List<RequirementHistoryResponse> getHistory(@PathVariable UUID id) {
        return auditService.getRequirementHistory(id).stream()
                .map(RequirementHistoryResponse::from)
                .toList();
    }

    @PostMapping("/{id}/archive")
    public RequirementResponse archive(@PathVariable UUID id) {
        return RequirementResponse.from(requirementService.archive(id));
    }

    @PostMapping("/{id}/relations")
    @ResponseStatus(HttpStatus.CREATED)
    public RelationResponse createRelation(@PathVariable UUID id, @Valid @RequestBody RelationRequest request) {
        return RelationResponse.from(requirementService.createRelation(id, request.targetId(), request.relationType()));
    }

    @GetMapping("/{id}/relations")
    public List<RelationResponse> getRelations(@PathVariable UUID id) {
        return requirementService.getRelations(id).stream()
                .map(RelationResponse::from)
                .toList();
    }

    @GetMapping("/{id}/relations/{relationId}/history")
    public List<RelationHistoryResponse> getRelationHistory(@PathVariable UUID id, @PathVariable UUID relationId) {
        return auditService.getRelationHistory(relationId).stream()
                .map(RelationHistoryResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}/relations/{relationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRelation(@PathVariable UUID id, @PathVariable UUID relationId) {
        requirementService.deleteRelation(id, relationId);
    }

    @GetMapping("/{id}/traceability")
    public List<TraceabilityLinkResponse> getTraceabilityLinks(@PathVariable UUID id) {
        return traceabilityService.getLinksForRequirement(id).stream()
                .map(TraceabilityLinkResponse::from)
                .toList();
    }

    @PostMapping("/{id}/traceability")
    @ResponseStatus(HttpStatus.CREATED)
    public TraceabilityLinkResponse createTraceabilityLink(
            @PathVariable UUID id, @Valid @RequestBody TraceabilityLinkRequest request) {
        var command = new CreateTraceabilityLinkCommand(
                request.artifactType(),
                request.artifactIdentifier(),
                request.artifactUrl(),
                request.artifactTitle(),
                request.linkType());
        return TraceabilityLinkResponse.from(traceabilityService.createLink(id, command));
    }

    @GetMapping("/{id}/traceability/{linkId}/history")
    public List<TraceabilityLinkHistoryResponse> getTraceabilityLinkHistory(
            @PathVariable UUID id, @PathVariable UUID linkId) {
        return auditService.getTraceabilityLinkHistory(linkId).stream()
                .map(TraceabilityLinkHistoryResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}/traceability/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTraceabilityLink(@PathVariable UUID id, @PathVariable UUID linkId) {
        traceabilityService.deleteLink(linkId);
    }
}
