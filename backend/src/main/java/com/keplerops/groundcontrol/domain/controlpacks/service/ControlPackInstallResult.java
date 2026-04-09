package com.keplerops.groundcontrol.domain.controlpacks.service;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPack;

public record ControlPackInstallResult(
        ControlPack controlPack,
        int controlsCreated,
        int controlsLinked,
        int entriesCreated,
        int mappingsCreated,
        int mappingsSkipped,
        boolean wasIdempotent) {}
