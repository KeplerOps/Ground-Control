package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.shared.security.ApiAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class ApiAuthenticationEntryPointTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(mapper);

    @Test
    void emits401WithErrorEnvelope() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var ex = new BadCredentialsException("token rejected");

        entryPoint.commence(request, response, ex);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        var body = mapper.readTree(response.getContentAsString());
        assertThat(body.path("error").path("code").asText()).isEqualTo("authentication_required");
        assertThat(body.path("error").path("message").asText()).isNotBlank();
    }

    @Test
    void setsWwwAuthenticateBearerHeader() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var ex = new BadCredentialsException("token rejected");

        entryPoint.commence(request, response, ex);

        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }

    @Test
    void doesNotLeakExceptionMessage_inResponseBody() throws Exception {
        // Don't echo the exception message — that can include internal details. The body should
        // be a stable, non-revealing message.
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var ex = new BadCredentialsException("internal:database:user-not-found:secret");

        entryPoint.commence(request, response, ex);

        assertThat(response.getContentAsString()).doesNotContain("internal:database");
    }
}
