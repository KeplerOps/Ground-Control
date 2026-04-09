package com.keplerops.groundcontrol.domain.controlpacks.service;

import com.keplerops.groundcontrol.domain.controlpacks.model.ControlPack;

public record ControlPackUpgradeResult(
        ControlPack controlPack,
        String previousVersion,
        int entriesAdded,
        int entriesUpdated,
        int entriesDeprecated,
        int controlsCreated,
        int controlsUpdated,
        int overridesPreserved) {}
