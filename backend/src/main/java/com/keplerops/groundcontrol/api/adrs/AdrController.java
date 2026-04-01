package com.keplerops.groundcontrol.api.adrs;

import com.keplerops.groundcontrol.api.requirements.RequirementResponse;
import com.keplerops.groundcontrol.domain.adrs.service.AdrService;
import com.keplerops.groundcontrol.domain.adrs.service.CreateAdrCommand;
import com.keplerops.groundcontrol.domain.adrs.service.UpdateAdrCommand;
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
@RequestMapping("/api/v1/adrs")
public class AdrController {

    private final AdrService adrService;
    private final ProjectService projectService;

    public AdrController(AdrService adrService, ProjectService projectService) {
        this.adrService = adrService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdrResponse create(@Valid @RequestBody AdrRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateAdrCommand(
                projectId,
                request.uid(),
                request.title(),
                request.decisionDate(),
                request.context(),
                request.decision(),
                request.consequences());
        return AdrResponse.from(adrService.create(command));
    }

    @GetMapping
    public List<AdrResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return adrService.listByProject(projectId).stream()
                .map(AdrResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AdrResponse getById(@PathVariable UUID id) {
        return AdrResponse.from(adrService.getById(id));
    }

    @GetMapping("/uid/{uid}")
    public AdrResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return AdrResponse.from(adrService.getByUid(uid, projectId));
    }

    @PutMapping("/{id}")
    public AdrResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAdrRequest request) {
        var command = new UpdateAdrCommand(
                request.title(),
                request.decisionDate(),
                request.context(),
                request.decision(),
                request.consequences(),
                request.supersededBy());
        return AdrResponse.from(adrService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        adrService.delete(id);
    }

    @PutMapping("/{id}/status")
    public AdrResponse transitionStatus(@PathVariable UUID id, @Valid @RequestBody AdrStatusTransitionRequest request) {
        return AdrResponse.from(adrService.transitionStatus(id, request.status()));
    }

    @GetMapping("/{id}/requirements")
    public List<RequirementResponse> getLinkedRequirements(@PathVariable UUID id) {
        return adrService.findLinkedRequirements(id).stream()
                .map(RequirementResponse::from)
                .toList();
    }
}
