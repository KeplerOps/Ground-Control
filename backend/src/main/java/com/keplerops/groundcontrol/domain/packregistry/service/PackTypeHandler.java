package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;

public interface PackTypeHandler {

    PackType packType();

    void applyRegistrationContent(PackRegistryEntry entry, PackRegistrationContent content);

    PackOperationResult install(PackOperationContext context);

    PackOperationResult upgrade(PackOperationContext context);
}
