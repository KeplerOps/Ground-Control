package com.keplerops.groundcontrol.domain.exception;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConflictException extends GroundControlException {

    private final Map<String, Serializable> detail;

    public ConflictException(String message) {
        this(message, "conflict", Map.of());
    }

    public ConflictException(String message, String errorCode, Map<String, ? extends Serializable> detail) {
        super(message, errorCode);
        this.detail = detail != null ? new LinkedHashMap<>(detail) : new LinkedHashMap<>();
    }

    public Map<String, Serializable> getDetail() {
        return Map.copyOf(detail);
    }
}
