package com.keplerops.groundcontrol.api.controlpacks;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPack;
import com.keplerops.groundcontrol.domain.controlpacks.state.ControlPackLifecycleState;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ControlPackResponse(
        UUID id,
        String projectIdentifier,
        String packId,
        String version,
        String publisher,
        String description,
        String sourceUrl,
        String checksum,
        Map<String, Object> compatibility,
        Map<String, Object> packMetadata,
        ControlPackLifecycleState lifecycleState,
        Instant installedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static ControlPackResponse from(ControlPack pack) {
        return new ControlPackResponse(
                pack.getId(),
                pack.getProject().getIdentifier(),
                pack.getPackId(),
                pack.getVersion(),
                pack.getPublisher(),
                pack.getDescription(),
                pack.getSourceUrl(),
                pack.getChecksum(),
                pack.getCompatibility(),
                pack.getPackMetadata(),
                pack.getLifecycleState(),
                pack.getInstalledAt(),
                pack.getCreatedAt(),
                pack.getUpdatedAt());
    }
}
