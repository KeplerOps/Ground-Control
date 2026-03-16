package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;
import java.util.UUID;

public record CreateGitHubIssueCommand(
        UUID projectId, String requirementUid, String repo, String extraBody, List<String> labels) {

    public CreateGitHubIssueCommand {
        labels = labels != null ? List.copyOf(labels) : List.of();
    }
}
