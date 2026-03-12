package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public record GitHubIssueData(int number, String title, String state, String url, String body, List<String> labels) {

    public GitHubIssueData {
        labels = List.copyOf(labels);
    }
}
