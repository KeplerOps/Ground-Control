package com.keplerops.groundcontrol.api.admin;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record GitHubIssueRequest(@NotBlank String requirementUid, String repo, String extraBody, List<String> labels) {}
