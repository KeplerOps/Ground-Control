package com.keplerops.groundcontrol.api.verification;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.verification.service.CreateVerificationResultCommand;
import com.keplerops.groundcontrol.domain.verification.service.UpdateVerificationResultCommand;
import com.keplerops.groundcontrol.domain.verification.service.VerificationResultService;
import com.keplerops.groundcontrol.domain.verification.state.VerificationStatus;
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
@RequestMapping("/api/v1/verification-results")
public class VerificationResultController {

    private final VerificationResultService verificationResultService;
    private final ProjectService projectService;

    public VerificationResultController(
            VerificationResultService verificationResultService, ProjectService projectService) {
        this.verificationResultService = verificationResultService;
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VerificationResultResponse create(
            @Valid @RequestBody VerificationResultRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        return VerificationResultResponse.from(verificationResultService.create(new CreateVerificationResultCommand(
                projectId,
                request.targetId(),
                request.requirementId(),
                request.prover(),
                request.property(),
                request.result(),
                request.assuranceLevel(),
                request.evidence(),
                request.verifiedAt(),
                request.expiresAt())));
    }

    @GetMapping
    public List<VerificationResultResponse> list(
            @RequestParam(required = false) String project,
            @RequestParam(name = "requirement_id", required = false) UUID requirementId,
            @RequestParam(required = false) String prover,
            @RequestParam(required = false) VerificationStatus result) {
        var projectId = projectService.resolveProjectId(project);
        return verificationResultService.listByProject(projectId, requirementId, prover, result).stream()
                .map(VerificationResultResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public VerificationResultResponse getById(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return VerificationResultResponse.from(verificationResultService.getById(projectId, id));
    }

    @PutMapping("/{id}")
    public VerificationResultResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVerificationResultRequest request,
            @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        return VerificationResultResponse.from(verificationResultService.update(
                projectId,
                id,
                new UpdateVerificationResultCommand(
                        request.targetId(),
                        request.requirementId(),
                        request.prover(),
                        request.property(),
                        request.result(),
                        request.assuranceLevel(),
                        request.evidence(),
                        request.verifiedAt(),
                        request.expiresAt())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(required = false) String project) {
        var projectId = projectService.requireProjectId(project);
        verificationResultService.delete(projectId, id);
    }
}
