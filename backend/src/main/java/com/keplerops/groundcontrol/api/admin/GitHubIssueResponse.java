package com.keplerops.groundcontrol.api.admin;

import com.keplerops.groundcontrol.domain.requirements.service.CreateGitHubIssueResult;
import java.util.UUID;

public record GitHubIssueResponse(String issueUrl, int issueNumber, UUID traceabilityLinkId, String warning) {

    public static GitHubIssueResponse from(CreateGitHubIssueResult result) {
        return new GitHubIssueResponse(
                result.issueUrl(), result.issueNumber(), result.traceabilityLinkId(), result.warning());
    }
}
