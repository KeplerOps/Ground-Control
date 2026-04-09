package com.keplerops.groundcontrol.api.controlpacks;

import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackInstallResult;

public record ControlPackInstallResultResponse(
        ControlPackResponse controlPack,
        int controlsCreated,
        int controlsLinked,
        int entriesCreated,
        int mappingsCreated,
        int mappingsSkipped,
        boolean wasIdempotent) {

    public static ControlPackInstallResultResponse from(ControlPackInstallResult result) {
        return new ControlPackInstallResultResponse(
                ControlPackResponse.from(result.controlPack()),
                result.controlsCreated(),
                result.controlsLinked(),
                result.entriesCreated(),
                result.mappingsCreated(),
                result.mappingsSkipped(),
                result.wasIdempotent());
    }
}
