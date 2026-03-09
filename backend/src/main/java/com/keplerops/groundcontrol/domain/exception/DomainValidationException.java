package com.keplerops.groundcontrol.domain.exception;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class DomainValidationException extends GroundControlException {

    private final Map<String, Serializable> detail;

    public DomainValidationException(String message) {
        this(message, "validation_error", Map.of());
    }

    public DomainValidationException(String message, String errorCode, Map<String, ? extends Serializable> detail) {
        super(message, errorCode);
        this.detail = detail != null ? new LinkedHashMap<>(detail) : new LinkedHashMap<>();
    }

    public Map<String, Serializable> getDetail() {
        return Map.copyOf(detail);
    }
}
