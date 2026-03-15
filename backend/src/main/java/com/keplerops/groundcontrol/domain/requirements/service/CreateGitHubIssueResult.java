package com.keplerops.groundcontrol.domain.requirements.service;

import java.util.UUID;

public record CreateGitHubIssueResult(String issueUrl, int issueNumber, UUID traceabilityLinkId, String warning) {}
