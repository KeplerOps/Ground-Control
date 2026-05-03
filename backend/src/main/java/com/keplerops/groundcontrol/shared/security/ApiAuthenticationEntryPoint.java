package com.keplerops.groundcontrol.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.shared.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Emits a 401 with the standard {@link ErrorResponse} envelope when an unauthenticated request
 * hits a protected route. Sends {@code WWW-Authenticate: Bearer} so spec-compliant clients know
 * which scheme to use. Never echoes the underlying exception message — that can leak internals.
 */
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String STABLE_MESSAGE = "Authentication is required to access this resource";

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authEx)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        var body = ErrorResponse.of("authentication_required", STABLE_MESSAGE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
