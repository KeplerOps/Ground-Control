package com.keplerops.groundcontrol.api.controls;

import com.keplerops.groundcontrol.domain.controls.service.ControlLinkService;
import com.keplerops.groundcontrol.domain.controls.state.ControlLinkTargetType;
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
@RequestMapping("/api/v1/controls/{controlId}/links")
public class ControlLinkController {

    private final ControlLinkService controlLinkService;
    private final ProjectService projectService;

    public ControlLinkController(ControlLinkService controlLinkService, ProjectService projectService) {
        this.controlLinkService = controlLinkService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ControlLinkResponse create(
            @PathVariable UUID controlId,
            @Valid @RequestBody ControlLinkRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return ControlLinkResponse.from(controlLinkService.create(
                projectId,
                controlId,
                request.targetType(),
                request.targetEntityId(),
                request.targetIdentifier(),
                request.linkType(),
                request.targetUrl(),
                request.targetTitle()));
    }

    @GetMapping
    public List<ControlLinkResponse> list(
            @PathVariable UUID controlId,
            @RequestParam(name = "target_type", required = false) ControlLinkTargetType targetType,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return controlLinkService.listByControl(projectId, controlId, targetType).stream()
                .map(ControlLinkResponse::from)
                .toList();
    }

    @DeleteMapping("/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID controlId, @PathVariable UUID linkId, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        controlLinkService.delete(projectId, controlId, linkId);
    }
}
