package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueSyncService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/sync")
public class SyncController {

    private final GitHubIssueSyncService syncService;

    public SyncController(GitHubIssueSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/github")
    public SyncResultResponse syncGithub(
            @RequestParam @NotBlank @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*$") String owner,
            @RequestParam @NotBlank @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*$") String repo) {
        return SyncResultResponse.from(syncService.syncGitHubIssues(owner, repo));
    }

    @PostMapping("/github/prs")
    public PrSyncResultResponse syncGithubPrs(
            @RequestParam @NotBlank @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*$") String owner,
            @RequestParam @NotBlank @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*$") String repo) {
        return PrSyncResultResponse.from(syncService.syncGitHubPullRequests(owner, repo));
    }
}
