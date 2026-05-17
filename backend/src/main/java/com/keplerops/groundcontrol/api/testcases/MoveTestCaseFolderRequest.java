package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record MoveTestCaseFolderRequest(UUID parentFolderId, @PositiveOrZero Integer sortOrder) {}
