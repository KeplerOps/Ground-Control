package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import java.util.UUID;

public record PackOperationContext(
        UUID projectId,
        PackRegistryEntry entry,
        ResolvedPack resolvedPack,
        PackIntegrityVerification integrityVerification) {}
