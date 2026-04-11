package com.keplerops.groundcontrol.api.packregistry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolvePackRequest(@NotBlank @Size(max = 200) String packId, @Size(max = 100) String versionConstraint) {}
