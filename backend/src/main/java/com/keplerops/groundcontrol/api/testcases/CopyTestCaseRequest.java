package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CopyTestCaseRequest(
        @NotBlank @Size(max = 50) String newUid, UUID parentFolderId, @PositiveOrZero Integer sortOrder) {}
