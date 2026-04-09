package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.api.controlpacks.ControlPackEntryDefinitionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InstallPackRequest(
        @NotBlank @Size(max = 200) String packId,
        @Size(max = 100) String versionConstraint,
        @Size(max = 255) String performedBy,
        @NotNull @Size(min = 1) @Valid List<ControlPackEntryDefinitionRequest> entries) {}
