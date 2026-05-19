package com.keplerops.groundcontrol.shared.security;

import java.util.LinkedHashMap;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Builds the {@link AuthenticationEntryPoint} for the ADR-037 browser chain.
 *
 * <p>{@code /api/v1/**} requests (the SPA's XHRs) get the canonical JSON envelope from
 * {@link ApiAuthenticationEntryPoint} so client-side fetch wrappers can detect the 401 and route
 * the user to {@code /login}. Anything else (browser navigation to SPA routes, the SPA shell on
 * a fresh session) is redirected to the form login at {@code /login}.
 *
 * <p>This is a static factory rather than a {@code @Configuration} bean because the wiring is
 * straightforward and the small surface area keeps the entry point's path policy in one readable
 * place.
 */
public final class DelegatingAuthenticationEntryPointFactory {

    private DelegatingAuthenticationEntryPointFactory() {
        // factory
    }

    public static AuthenticationEntryPoint build(ApiAuthenticationEntryPoint apiEntryPoint, String loginPage) {
        if (loginPage == null || loginPage.isBlank()) {
            throw new IllegalArgumentException("loginPage must be a non-blank path (e.g. \"/login\")");
        }
        var loginRedirect = new LoginUrlAuthenticationEntryPoint(loginPage);
        var entryPoints = new LinkedHashMap<RequestMatcher, AuthenticationEntryPoint>();
        // Shared classification — see ApiRequestPaths. Keeping the predicate in one place
        // prevents the 401 entry point, the 403 access-denied handler, and the request cache
        // exclusion from drifting against one another.
        entryPoints.put(ApiRequestPaths::isApiRequest, apiEntryPoint);
        var delegating = new DelegatingAuthenticationEntryPoint(entryPoints);
        delegating.setDefaultEntryPoint(loginRedirect);
        return delegating;
    }
}
