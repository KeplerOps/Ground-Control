package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.service.CreateRiskRegisterRecordCommand;
import com.keplerops.groundcontrol.domain.riskscenarios.service.RiskRegisterRecordService;
import com.keplerops.groundcontrol.domain.riskscenarios.service.UpdateRiskRegisterRecordCommand;
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
@RequestMapping("/api/v1/risk-register-records")
public class RiskRegisterRecordController {

    private final RiskRegisterRecordService riskRegisterRecordService;
    private final ProjectService projectService;

    public RiskRegisterRecordController(
            RiskRegisterRecordService riskRegisterRecordService, ProjectService projectService) {
        this.riskRegisterRecordService = riskRegisterRecordService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RiskRegisterRecordResponse create(
            @Valid @RequestBody RiskRegisterRecordRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return RiskRegisterRecordResponse.from(riskRegisterRecordService.create(new CreateRiskRegisterRecordCommand(
                projectId,
                request.uid(),
                request.title(),
                request.owner(),
                request.reviewCadence(),
                request.nextReviewAt(),
                request.categoryTags(),
                request.decisionMetadata(),
                request.assetScopeSummary(),
                request.riskScenarioIds())));
    }

    @GetMapping
    public List<RiskRegisterRecordResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return riskRegisterRecordService.listByProject(projectId).stream()
                .map(RiskRegisterRecordResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public RiskRegisterRecordResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return RiskRegisterRecordResponse.from(riskRegisterRecordService.getById(projectId, id));
    }

    @PutMapping("/{id}")
    public RiskRegisterRecordResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRiskRegisterRecordRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return RiskRegisterRecordResponse.from(riskRegisterRecordService.update(
                projectId,
                id,
                new UpdateRiskRegisterRecordCommand(
                        request.title(),
                        request.owner(),
                        request.reviewCadence(),
                        request.nextReviewAt(),
                        request.categoryTags(),
                        request.decisionMetadata(),
                        request.assetScopeSummary(),
                        request.riskScenarioIds())));
    }

    @PutMapping("/{id}/status")
    public RiskRegisterRecordResponse transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody RiskRegisterStatusTransitionRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return RiskRegisterRecordResponse.from(
                riskRegisterRecordService.transitionStatus(projectId, id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        riskRegisterRecordService.delete(projectId, id);
    }
}
