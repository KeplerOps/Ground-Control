package com.keplerops.groundcontrol.api.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GitHubIssueRequest(
        @NotBlank String requirementUid,
        @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*/[a-zA-Z0-9][a-zA-Z0-9._-]*$") String repo,
        @Size(max = 65536) String extraBody,
        List<@Size(max = 50) String> labels) {}
