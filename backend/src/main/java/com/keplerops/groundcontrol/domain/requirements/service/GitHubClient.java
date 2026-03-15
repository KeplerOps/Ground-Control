package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.List;

public interface GitHubClient {

    List<GitHubIssueData> fetchAllIssues(String owner, String repo);

    GitHubIssueData createIssue(String repo, String title, String body, List<String> labels);
}
