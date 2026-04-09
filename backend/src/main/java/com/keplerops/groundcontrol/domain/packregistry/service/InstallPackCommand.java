package com.keplerops.groundcontrol.domain.packregistry.service;

import java.util.UUID;

public record InstallPackCommand(UUID projectId, String packId, String versionConstraint, String performedBy) {}
