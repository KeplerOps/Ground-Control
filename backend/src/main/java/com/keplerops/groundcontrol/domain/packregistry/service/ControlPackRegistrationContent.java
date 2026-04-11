package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.controlpacks.service.ControlPackEntryDefinition;
import java.util.List;

public record ControlPackRegistrationContent(List<ControlPackEntryDefinition> entries)
        implements PackRegistrationContent {

    public ControlPackRegistrationContent {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
