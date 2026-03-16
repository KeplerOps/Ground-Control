package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.projects.service.ProjectService;
import com.keplerops.groundcontrol.domain.requirements.service.CreateGitHubIssueCommand;
import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueSyncService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/github")
public class GitHubIssueController {

    private final GitHubIssueSyncService syncService;
    private final ProjectService projectService;

    public GitHubIssueController(GitHubIssueSyncService syncService, ProjectService projectService) {
        this.syncService = syncService;
        this.projectService = projectService;
    }

    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public GitHubIssueResponse createIssue(
            @Valid @RequestBody GitHubIssueRequest request, @RequestParam(required = false) String project) {
        var projectId = projectService.resolveProjectId(project);
        var command = new CreateGitHubIssueCommand(
                projectId,
                request.requirementUid(),
                request.repo(),
                request.extraBody(),
                request.labels() != null ? request.labels() : List.of());
        return GitHubIssueResponse.from(syncService.createIssueFromRequirement(command));
    }
}
