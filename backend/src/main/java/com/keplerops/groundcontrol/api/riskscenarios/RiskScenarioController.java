package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.api.requirements.RequirementResponse;
import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.service.CreateRiskScenarioCommand;
import com.keplerops.groundcontrol.domain.riskscenarios.service.RiskScenarioService;
import com.keplerops.groundcontrol.domain.riskscenarios.service.UpdateRiskScenarioCommand;
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
@RequestMapping("/api/v1/risk-scenarios")
public class RiskScenarioController {

    private final RiskScenarioService riskScenarioService;
    private final ProjectService projectService;

    public RiskScenarioController(RiskScenarioService riskScenarioService, ProjectService projectService) {
        this.riskScenarioService = riskScenarioService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RiskScenarioResponse create(
            @Valid @RequestBody RiskScenarioRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateRiskScenarioCommand(
                projectId,
                request.uid(),
                request.title(),
                request.threatSource(),
                request.threatEvent(),
                request.affectedObject(),
                request.vulnerability(),
                request.consequence(),
                request.timeHorizon(),
                request.observationRefs(),
                request.topologyContext());
        return RiskScenarioResponse.from(riskScenarioService.create(command));
    }

    @GetMapping
    public List<RiskScenarioResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return riskScenarioService.listByProject(projectId).stream()
                .map(RiskScenarioResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public RiskScenarioResponse getById(@PathVariable UUID id) {
        return RiskScenarioResponse.from(riskScenarioService.getById(id));
    }

    @GetMapping("/uid/{uid}")
    public RiskScenarioResponse getByUid(@PathVariable String uid, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return RiskScenarioResponse.from(riskScenarioService.getByUid(uid, projectId));
    }

    @PutMapping("/{id}")
    public RiskScenarioResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRiskScenarioRequest request) {
        var command = new UpdateRiskScenarioCommand(
                request.title(),
                request.threatSource(),
                request.threatEvent(),
                request.affectedObject(),
                request.vulnerability(),
                request.consequence(),
                request.timeHorizon(),
                request.observationRefs(),
                request.topologyContext());
        return RiskScenarioResponse.from(riskScenarioService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        riskScenarioService.delete(id);
    }

    @PutMapping("/{id}/status")
    public RiskScenarioResponse transitionStatus(
            @PathVariable UUID id, @Valid @RequestBody RiskScenarioStatusTransitionRequest request) {
        return RiskScenarioResponse.from(riskScenarioService.transitionStatus(id, request.status()));
    }

    @GetMapping("/{id}/requirements")
    public List<RequirementResponse> getLinkedRequirements(@PathVariable UUID id) {
        return riskScenarioService.findLinkedRequirements(id).stream()
                .map(RequirementResponse::from)
                .toList();
    }
}
