package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.packregistry.service.ResolvedPack;
import java.util.List;

public record ResolvedPackResponse(
        PackRegistryEntryResponse entry,
        String resolvedVersion,
        String resolvedSource,
        String resolvedChecksum,
        boolean compatible,
        List<ResolvedPackResponse> resolvedDependencies) {

    public static ResolvedPackResponse from(ResolvedPack resolved, boolean compatible) {
        return new ResolvedPackResponse(
                PackRegistryEntryResponse.from(resolved.entry()),
                resolved.resolvedVersion(),
                resolved.resolvedSource(),
                resolved.resolvedChecksum(),
                compatible,
                resolved.resolvedDependencies() != null
                        ? resolved.resolvedDependencies().stream()
                                .map(d -> ResolvedPackResponse.from(d, true))
                                .toList()
                        : List.of());
    }
}
