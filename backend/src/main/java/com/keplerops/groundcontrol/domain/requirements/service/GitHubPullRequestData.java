package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record GitHubPullRequestData(
        int number,
        String title,
        String state,
        boolean merged,
        String url,
        String body,
        String baseBranch,
        String headBranch,
        List<String> labels) {

    public GitHubPullRequestData {
        labels = List.copyOf(labels);
    }
}
