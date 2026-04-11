package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import java.util.List;

public record ResolvedPack(
        PackRegistryEntry entry,
        String resolvedVersion,
        String resolvedSource,
        String resolvedChecksum,
        List<ResolvedPack> resolvedDependencies) {}
