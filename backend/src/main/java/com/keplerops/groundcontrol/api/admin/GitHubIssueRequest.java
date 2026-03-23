package com.keplerops.groundcontrol.api.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GitHubIssueRequest(
        @NotBlank @Size(max = 50) String requirementUid,
        @Size(max = 200) String repo,
        @Size(max = 50000) String extraBody,
        @Size(max = 20) List<@Size(max = 50) String> labels) {}
