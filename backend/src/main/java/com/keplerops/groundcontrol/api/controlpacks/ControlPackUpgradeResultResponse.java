package com.keplerops.groundcontrol.api.controlpacks;

import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackUpgradeResult;

public record ControlPackUpgradeResultResponse(
        ControlPackResponse controlPack,
        String previousVersion,
        int entriesAdded,
        int entriesUpdated,
        int entriesDeprecated,
        int controlsCreated,
        int controlsUpdated,
        int overridesPreserved) {

    public static ControlPackUpgradeResultResponse from(ControlPackUpgradeResult result) {
        return new ControlPackUpgradeResultResponse(
                ControlPackResponse.from(result.controlPack()),
                result.previousVersion(),
                result.entriesAdded(),
                result.entriesUpdated(),
                result.entriesDeprecated(),
                result.controlsCreated(),
                result.controlsUpdated(),
                result.overridesPreserved());
    }
}
