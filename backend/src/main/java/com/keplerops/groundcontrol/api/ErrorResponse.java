package com.keplerops.groundcontrol.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Structured error response envelope.
 *
 * <pre>
 * {"error": {"code": "not_found", "message": "...", "detail": null}}
 * </pre>
 */
public record ErrorResponse(ErrorBody error) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(String code, String message, Map<String, Object> detail) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message, null));
    }

    public static ErrorResponse of(String code, String message, Map<String, Object> detail) {
        return new ErrorResponse(new ErrorBody(code, message, detail));
    }
}
