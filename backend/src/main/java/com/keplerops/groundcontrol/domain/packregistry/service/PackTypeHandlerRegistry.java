package com.keplerops.groundcontrol.domain.packregistry.service;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.packregistry.state.PackType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PackTypeHandlerRegistry {

    private final Map<PackType, PackTypeHandler> handlersByType;

    public PackTypeHandlerRegistry(List<PackTypeHandler> handlers) {
        this.handlersByType = new EnumMap<>(PackType.class);
        for (var handler : handlers) {
            var existing = handlersByType.put(handler.packType(), handler);
            if (existing != null) {
                throw new IllegalStateException("Duplicate pack type handler registered for " + handler.packType());
            }
        }
    }

    public PackTypeHandler get(PackType packType) {
        var handler = handlersByType.get(packType);
        if (handler == null) {
            throw new DomainValidationException("No pack type handler registered for " + packType);
        }
        return handler;
    }
}
