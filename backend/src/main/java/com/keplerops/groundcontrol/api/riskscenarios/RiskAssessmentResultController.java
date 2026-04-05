package com.keplerops.groundcontrol.api.riskscenarios;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.riskscenarios.service.CreateRiskAssessmentResultCommand;
import com.keplerops.groundcontrol.domain.riskscenarios.service.RiskAssessmentResultService;
import com.keplerops.groundcontrol.domain.riskscenarios.service.UpdateRiskAssessmentResultCommand;
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
@RequestMapping("/api/v1/risk-assessment-results")
public class RiskAssessmentResultController {

    private final RiskAssessmentResultService riskAssessmentResultService;
    private final ProjectService projectService;

    public RiskAssessmentResultController(
            RiskAssessmentResultService riskAssessmentResultService, ProjectService projectService) {
        this.riskAssessmentResultService = riskAssessmentResultService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RiskAssessmentResultResponse create(
            @Valid @RequestBody RiskAssessmentResultRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return RiskAssessmentResultResponse.from(
                riskAssessmentResultService.create(new CreateRiskAssessmentResultCommand(
                        projectId,
                        request.riskScenarioId(),
                        request.riskRegisterRecordId(),
                        request.methodologyProfileId(),
                        request.analystIdentity(),
                        request.assumptions(),
                        request.inputFactors(),
                        request.observationDate(),
                        request.assessmentAt(),
                        request.timeHorizon(),
                        request.confidence(),
                        request.uncertaintyMetadata(),
                        request.computedOutputs(),
                        request.evidenceRefs(),
                        request.notes(),
                        request.observationIds())));
    }

    @GetMapping
    public List<RiskAssessmentResultResponse> list(
            @RequestParam(required = false) UUID riskScenarioId,
            @RequestParam(required = false) UUID riskRegisterRecordId,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        if (riskScenarioId != null) {
            return riskAssessmentResultService.listByScenario(projectId, riskScenarioId).stream()
                    .map(RiskAssessmentResultResponse::from)
                    .toList();
        }
        if (riskRegisterRecordId != null) {
            return riskAssessmentResultService.listByRiskRegisterRecord(projectId, riskRegisterRecordId).stream()
                    .map(RiskAssessmentResultResponse::from)
                    .toList();
        }
        return riskAssessmentResultService.listByProject(projectId).stream()
                .map(RiskAssessmentResultResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public RiskAssessmentResultResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return RiskAssessmentResultResponse.from(riskAssessmentResultService.getById(projectId, id));
    }

    @PutMapping("/{id}")
    public RiskAssessmentResultResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRiskAssessmentResultRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return RiskAssessmentResultResponse.from(riskAssessmentResultService.update(
                projectId,
                id,
                new UpdateRiskAssessmentResultCommand(
                        request.riskRegisterRecordId(),
                        request.methodologyProfileId(),
                        request.analystIdentity(),
                        request.assumptions(),
                        request.inputFactors(),
                        request.observationDate(),
                        request.assessmentAt(),
                        request.timeHorizon(),
                        request.confidence(),
                        request.uncertaintyMetadata(),
                        request.computedOutputs(),
                        request.evidenceRefs(),
                        request.notes(),
                        request.observationIds())));
    }

    @PutMapping("/{id}/approval-state")
    public RiskAssessmentResultResponse transitionApprovalState(
            @PathVariable UUID id,
            @Valid @RequestBody RiskAssessmentApprovalStateTransitionRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return RiskAssessmentResultResponse.from(
                riskAssessmentResultService.transitionApprovalState(projectId, id, request.approvalState()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        riskAssessmentResultService.delete(projectId, id);
    }
}
