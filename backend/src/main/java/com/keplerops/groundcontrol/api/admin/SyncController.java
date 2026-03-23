package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueSyncService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/sync")
@Validated
public class SyncController {

    /** GitHub owner/repo names: alphanumeric, hyphens, underscores, dots. */
    private static final String GITHUB_NAME_PATTERN = "[a-zA-Z0-9._-]+";

    private final GitHubIssueSyncService syncService;

    public SyncController(GitHubIssueSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/github")
    public SyncResultResponse syncGithub(
            @RequestParam @Pattern(regexp = GITHUB_NAME_PATTERN) String owner,
            @RequestParam @Pattern(regexp = GITHUB_NAME_PATTERN) String repo) {
        return SyncResultResponse.from(syncService.syncGitHubIssues(owner, repo));
    }
}
