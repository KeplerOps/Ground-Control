package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TestCaseFolderRequest(
        UUID parentFolderId,
        @NotBlank @Size(max = 200) String title,
        String description,
        @PositiveOrZero Integer sortOrder) {}
