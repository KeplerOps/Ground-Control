package com.keplerops.groundcontrol.api.testcases;

import jakarta.validation.constraints.Size;

public record UpdateTestCaseFolderRequest(
        @Size(max = 200) String title, String description, Boolean clearDescription) {}
