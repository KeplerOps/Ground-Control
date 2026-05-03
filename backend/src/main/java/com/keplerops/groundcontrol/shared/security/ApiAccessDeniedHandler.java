package com.keplerops.groundcontrol.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Emits a 403 with the standard {@link ErrorResponse} envelope when an authenticated caller is
 * missing the required role. Never echoes the underlying exception message.
 */
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private static final String STABLE_MESSAGE = "You do not have permission to access this resource";

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        var body = ErrorResponse.of("access_denied", STABLE_MESSAGE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
