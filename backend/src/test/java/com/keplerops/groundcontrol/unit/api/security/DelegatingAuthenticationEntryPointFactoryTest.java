package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.shared.security.ApiAuthenticationEntryPoint;
import com.keplerops.groundcontrol.shared.security.DelegatingAuthenticationEntryPointFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

/**
 * Asserts the browser chain's entry point delegates {@code /api/v1/**} to the JSON envelope and
 * redirects everything else to {@code /login}. The two-way behavior is what keeps the SPA's XHR
 * calls returning 401 JSON while normal browser navigation still hits the form.
 */
class DelegatingAuthenticationEntryPointFactoryTest {

    @Test
    void apiPathReturns401JsonEnvelope() throws Exception {
        var entryPoint = DelegatingAuthenticationEntryPointFactory.build(
                new ApiAuthenticationEntryPoint(new ObjectMapper()), "/login");

        var request = new MockHttpServletRequest("GET", "/api/v1/requirements");
        var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new StubAuthException());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("authentication_required");
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getRedirectedUrl()).isNull();
    }

    @Test
    void spaPathRedirectsToLogin() throws Exception {
        var entryPoint = DelegatingAuthenticationEntryPointFactory.build(
                new ApiAuthenticationEntryPoint(new ObjectMapper()), "/login");

        var request = new MockHttpServletRequest("GET", "/requirements");
        var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new StubAuthException());

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).endsWith("/login");
    }

    @Test
    void rootPathRedirectsToLogin() throws Exception {
        var entryPoint = DelegatingAuthenticationEntryPointFactory.build(
                new ApiAuthenticationEntryPoint(new ObjectMapper()), "/login");

        var request = new MockHttpServletRequest("GET", "/");
        var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new StubAuthException());

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).endsWith("/login");
    }

    private static final class StubAuthException extends AuthenticationException {
        StubAuthException() {
            super("unauthenticated");
        }
    }
}
