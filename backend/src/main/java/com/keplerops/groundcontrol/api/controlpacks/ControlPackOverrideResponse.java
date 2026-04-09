package com.keplerops.groundcontrol.api.controlpacks;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPackOverride;
import java.time.Instant;
import java.util.UUID;

public record ControlPackOverrideResponse(
        UUID id,
        UUID controlPackEntryId,
        String fieldName,
        String overrideValue,
        String reason,
        Instant createdAt,
        Instant updatedAt) {

    public static ControlPackOverrideResponse from(ControlPackOverride override) {
        return new ControlPackOverrideResponse(
                override.getId(),
                override.getControlPackEntry().getId(),
                override.getFieldName(),
                override.getOverrideValue(),
                override.getReason(),
                override.getCreatedAt(),
                override.getUpdatedAt());
    }
}
