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

    public static ErrorResponse of(String code, String message, Map<String, ?> detail) {
        // Treat null/empty detail the same as the 2-arg overload so callers don't
        // have to choose between the two when they have a possibly-empty map. Without
        // this guard `Map.copyOf(null)` would NPE and an empty map would serialize
        // as `detail: {}` instead of being omitted by @JsonInclude(NON_NULL).
        if (detail == null || detail.isEmpty()) {
            return of(code, message);
        }
        return new ErrorResponse(new ErrorBody(code, message, Map.copyOf(detail)));
    }
}
