package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import org.springframework.stereotype.Service;

@Service
public class RequirementsPackTypeHandler extends AbstractMetadataOnlyPackTypeHandler {

    @Override
    public PackType packType() {
        return PackType.REQUIREMENTS_PACK;
    }
}
