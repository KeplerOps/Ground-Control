package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition;
import java.util.List;
import java.util.UUID;

public record InstallPackCommand(
        UUID projectId,
        String packId,
        String versionConstraint,
        String performedBy,
        List<ControlPackEntryDefinition> entries) {}
