package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.GitHubIssueSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/sync")
public class SyncController {

    private final GitHubIssueSyncService syncService;

    public SyncController(GitHubIssueSyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/github")
    public SyncResultResponse syncGithub(@RequestParam String owner, @RequestParam String repo) {
        return SyncResultResponse.from(syncService.syncGitHubIssues(owner, repo));
    }
}
