package com.keplerops.groundcontrol.api.controlpacks;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPackEntry;
import com.keplerops.groundcontrol.domain.controlpacks.state.ControlPackEntryStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ControlPackEntryResponse(
        UUID id,
        UUID controlPackId,
        UUID controlId,
        String entryUid,
        Map<String, Object> originalDefinition,
        List<Map<String, Object>> expectedEvidence,
        String implementationGuidance,
        List<Map<String, Object>> frameworkMappings,
        ControlPackEntryStatus entryStatus,
        Instant createdAt,
        Instant updatedAt) {

    public static ControlPackEntryResponse from(ControlPackEntry entry) {
        return new ControlPackEntryResponse(
                entry.getId(),
                entry.getControlPack().getId(),
                entry.getControl().getId(),
                entry.getEntryUid(),
                entry.getOriginalDefinition(),
                entry.getExpectedEvidence(),
                entry.getImplementationGuidance(),
                entry.getFrameworkMappings(),
                entry.getEntryStatus(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }
}
