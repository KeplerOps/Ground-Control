package com.keplerops.groundcontrol.api.requirements;

import com.keplerops.groundcontrol.domain.requirements.service.CreateRequirementCommand;
import com.keplerops.groundcontrol.domain.requirements.service.RequirementService;
import com.keplerops.groundcontrol.domain.requirements.service.UpdateRequirementCommand;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementResponse create(@Valid @RequestBody RequirementRequest request) {
        var command = new CreateRequirementCommand(
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
    public Page<RequirementResponse> list(Pageable pageable) {
        return requirementService.list(pageable).map(RequirementResponse::from);
    }

    @GetMapping("/{id}")
    public RequirementResponse getById(@PathVariable UUID id) {
        return RequirementResponse.from(requirementService.getById(id));
    }

    @GetMapping("/uid/{uid}")
    public RequirementResponse getByUid(@PathVariable String uid) {
        return RequirementResponse.from(requirementService.getByUid(uid));
    }

    @PutMapping("/{id}")
    public RequirementResponse update(@PathVariable UUID id, @Valid @RequestBody RequirementRequest request) {
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
}
