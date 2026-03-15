package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record CreateGitHubIssueCommand(String requirementUid, String repo, String extraBody, List<String> labels) {

    public CreateGitHubIssueCommand {
        labels = labels != null ? List.copyOf(labels) : List.of();
    }
}
