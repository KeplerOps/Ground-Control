package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.packregistry.model.PackRegistryEntry;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;

abstract class AbstractMetadataOnlyPackTypeHandler implements PackTypeHandler {

    @Override
    public void applyRegistrationContent(PackRegistryEntry entry, PackRegistrationContent content) {
        if (!(content instanceof EmptyPackRegistrationContent)) {
            throw new DomainValidationException(packType() + " registry entries do not accept typed content yet");
        }
    }

    @Override
    public PackOperationResult install(PackOperationContext context) {
        throw new DomainValidationException(packType() + " installation is not supported yet");
    }

    @Override
    public PackOperationResult upgrade(PackOperationContext context) {
        throw new DomainValidationException(packType() + " upgrade is not supported yet");
    }

    @Override
    public abstract PackType packType();
}
