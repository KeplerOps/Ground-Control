package com.keplerops.groundcontrol.api.qualitygates;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.qualitygates.service.CreateQualityGateCommand;
import com.keplerops.groundcontrol.domain.qualitygates.service.QualityGateService;
import com.keplerops.groundcontrol.domain.qualitygates.service.UpdateQualityGateCommand;
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
@RequestMapping("/api/v1/quality-gates")
public class QualityGateController {

    private final QualityGateService qualityGateService;
    private final ProjectService projectService;

    public QualityGateController(QualityGateService qualityGateService, ProjectService projectService) {
        this.qualityGateService = qualityGateService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QualityGateResponse create(
            @Valid @RequestBody QualityGateRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateQualityGateCommand(
                projectId,
                request.name(),
                request.description(),
                request.metricType(),
                request.metricParam(),
                request.scopeStatus(),
                request.operator(),
                request.threshold());
        return QualityGateResponse.from(qualityGateService.create(command));
    }

    @GetMapping
    public List<QualityGateResponse> list(@RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return qualityGateService.listByProject(projectId).stream()
                .map(QualityGateResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public QualityGateResponse getById(@PathVariable UUID id) {
        return QualityGateResponse.from(qualityGateService.getById(id));
    }

    @PutMapping("/{id}")
    public QualityGateResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateQualityGateRequest request) {
        var command = new UpdateQualityGateCommand(
                request.name(),
                request.description(),
                request.metricType(),
                request.metricParam(),
                request.scopeStatus(),
                request.operator(),
                request.threshold(),
                request.enabled());
        return QualityGateResponse.from(qualityGateService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        qualityGateService.delete(id);
    }

    @PostMapping("/evaluate")
    public QualityGateEvaluationResponse evaluate(@RequestParam(required = false) String project) {
        return QualityGateEvaluationResponse.from(qualityGateService.evaluate(project));
    }
}
