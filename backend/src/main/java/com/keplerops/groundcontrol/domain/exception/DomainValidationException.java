package com.keplerops.groundcontrol.domain.exception;

import java.util.Map;

public class DomainValidationException extends GroundControlException {

    private final Map<String, Object> detail;

    public DomainValidationException(String message) {
        this(message, "validation_error", Map.of());
    }

    public DomainValidationException(String message, String errorCode, Map<String, Object> detail) {
        super(message, errorCode);
        this.detail = detail != null ? Map.copyOf(detail) : Map.of();
    }

    public Map<String, Object> getDetail() {
        return detail;
    }
}
