package com.keplerops.groundcontrol.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * The ADR-037 chain discriminator. The API filter chain ({@link ApiSecurityConfig}) is scoped to
 * only those requests that present an {@code Authorization: Bearer …} header so machine /
 * automation traffic keeps the existing ADR-026 stateless behavior (no CSRF, no session, JSON
 * envelopes). Every other request — browser navigation, SPA XHRs with the session cookie,
 * logins, logouts — falls through to the browser chain.
 *
 * <p>Matching is by HTTP scheme only ({@code Bearer} prefix, case-insensitive). Token validity
 * is the responsibility of {@link BearerTokenAuthFilter}; if the chain matches but the token is
 * invalid the API entry point still emits a JSON 401 instead of redirecting to {@code /login}.
 */
public final class BearerRequestMatcher implements RequestMatcher {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean matches(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null) {
            return false;
        }
        if (header.length() < BEARER_PREFIX.length()) {
            return false;
        }
        return header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length());
    }
}
