package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.service.ControlEffectivenessAssessmentService;
import com.keplerops.groundcontrol.domain.controls.service.CreateControlEffectivenessAssessmentCommand;
import com.keplerops.groundcontrol.domain.controls.service.UpdateControlEffectivenessAssessmentCommand;
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
@RequestMapping("/api/v1/control-effectiveness-assessments")
public class ControlEffectivenessAssessmentController {

    private final ControlEffectivenessAssessmentService service;
    private final ProjectService projectService;

    public ControlEffectivenessAssessmentController(
            ControlEffectivenessAssessmentService service, ProjectService projectService) {
        this.service = service;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ControlEffectivenessAssessmentResponse create(
            @Valid @RequestBody ControlEffectivenessAssessmentRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return ControlEffectivenessAssessmentResponse.from(
                service.create(new CreateControlEffectivenessAssessmentCommand(
                        projectId,
                        request.controlId(),
                        request.uid(),
                        request.designEffectiveness(),
                        request.operatingEffectiveness(),
                        request.assessedAt(),
                        request.assessor(),
                        request.rationale(),
                        request.notes(),
                        request.supportingTestIds())));
    }

    @GetMapping
    public List<ControlEffectivenessAssessmentResponse> list(
            @RequestParam(required = false) UUID controlId, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var rows = controlId == null
                ? service.listByProject(projectId)
                : service.listByProjectAndControl(projectId, controlId);
        return rows.stream().map(ControlEffectivenessAssessmentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ControlEffectivenessAssessmentResponse getById(
            @PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlEffectivenessAssessmentResponse.from(service.getById(projectId, id));
    }

    @PutMapping("/{id}")
    public ControlEffectivenessAssessmentResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateControlEffectivenessAssessmentRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlEffectivenessAssessmentResponse.from(service.update(
                projectId,
                id,
                new UpdateControlEffectivenessAssessmentCommand(
                        request.designEffectiveness(),
                        request.operatingEffectiveness(),
                        request.assessedAt(),
                        request.assessor(),
                        request.rationale(),
                        request.notes(),
                        request.supportingTestIds())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        service.delete(projectId, id);
    }
}
