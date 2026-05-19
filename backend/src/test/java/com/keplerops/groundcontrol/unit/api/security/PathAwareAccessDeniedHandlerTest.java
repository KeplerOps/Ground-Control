package com.keplerops.groundcontrol.unit.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keplerops.groundcontrol.shared.security.ApiAccessDeniedHandler;
import com.keplerops.groundcontrol.shared.security.PathAwareAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * The browser chain reuses {@link ApiAccessDeniedHandler}'s JSON envelope for {@code /api/v1/**}
 * requests (so SPA XHR 403s never end up as HTML pages) but lets non-API browser navigation
 * fall through to Spring's default. The handler under test is the path dispatcher.
 */
class PathAwareAccessDeniedHandlerTest {

    @Test
    void apiPathDelegatesToJsonHandler() throws Exception {
        var apiHandler = mock(ApiAccessDeniedHandler.class);
        var fallback = mock(AccessDeniedHandler.class);
        var handler = new PathAwareAccessDeniedHandler(apiHandler, fallback);

        var request = new MockHttpServletRequest("GET", "/api/v1/requirements");
        var response = new MockHttpServletResponse();
        var ex = new AccessDeniedException("denied");

        handler.handle(request, response, ex);

        verify(apiHandler).handle(request, response, ex);
        verify(fallback, never()).handle(any(), any(), any());
    }

    @Test
    void nonApiPathDelegatesToFallback() throws Exception {
        var apiHandler = mock(ApiAccessDeniedHandler.class);
        var fallback = mock(AccessDeniedHandler.class);
        var handler = new PathAwareAccessDeniedHandler(apiHandler, fallback);

        var request = new MockHttpServletRequest("GET", "/some/spa/page");
        var response = new MockHttpServletResponse();
        var ex = new AccessDeniedException("denied");

        handler.handle(request, response, ex);

        verify(fallback).handle(request, response, ex);
        verify(apiHandler, never()).handle(any(), any(), any());
    }

    @Test
    void apiPrefixMatchesEvenWithQueryString() throws Exception {
        var apiHandler = mock(ApiAccessDeniedHandler.class);
        var fallback = mock(AccessDeniedHandler.class);
        var handler = new PathAwareAccessDeniedHandler(apiHandler, fallback);

        var request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        request.setQueryString("page=1");
        var response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        verify(apiHandler).handle(any(), any(), any());
    }

    @Test
    void apiAccessDeniedHandlerEmits403JsonEnvelope() throws Exception {
        // Sanity check that the wrapped handler is still the canonical one — guards against
        // accidental swap to a raw 403 page during a future refactor.
        var apiHandler = new ApiAccessDeniedHandler(new ObjectMapper());
        var request = new MockHttpServletRequest("GET", "/api/v1/x");
        var response = new MockHttpServletResponse();

        apiHandler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("access_denied");
    }
}
