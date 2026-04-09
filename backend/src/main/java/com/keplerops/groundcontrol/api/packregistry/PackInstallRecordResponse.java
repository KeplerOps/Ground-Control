package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.packregistry.model.PackInstallRecord;
import com.keplerops.groundcontrol.domain.packregistry.state.InstallOutcome;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import com.keplerops.groundcontrol.domain.packregistry.state.TrustOutcome;
import java.time.Instant;
import java.util.UUID;

public record PackInstallRecordResponse(
        UUID id,
        String projectIdentifier,
        String packId,
        PackType packType,
        String requestedVersion,
        String resolvedVersion,
        String resolvedSource,
        String resolvedChecksum,
        Boolean signatureVerified,
        String trustPolicyId,
        TrustOutcome trustOutcome,
        String trustReason,
        InstallOutcome installOutcome,
        String errorDetail,
        UUID installedEntityId,
        Instant performedAt,
        String performedBy,
        Instant createdAt,
        Instant updatedAt) {

    public static PackInstallRecordResponse from(PackInstallRecord record) {
        return new PackInstallRecordResponse(
                record.getId(),
                record.getProject().getIdentifier(),
                record.getPackId(),
                record.getPackType(),
                record.getRequestedVersion(),
                record.getResolvedVersion(),
                record.getResolvedSource(),
                record.getResolvedChecksum(),
                record.getSignatureVerified(),
                record.getTrustPolicyId(),
                record.getTrustOutcome(),
                record.getTrustReason(),
                record.getInstallOutcome(),
                record.getErrorDetail(),
                record.getInstalledEntityId(),
                record.getPerformedAt(),
                record.getPerformedBy(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
