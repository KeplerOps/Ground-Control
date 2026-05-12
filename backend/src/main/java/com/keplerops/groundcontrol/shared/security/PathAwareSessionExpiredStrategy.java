package com.keplerops.groundcontrol.shared.security;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;

/**
 * Bridges Spring Security's {@link org.springframework.security.web.authentication.session.ConcurrentSessionFilter
 * ConcurrentSessionFilter} expiry event into the ADR-037 path policy. When
 * {@link org.springframework.security.core.session.SessionInformation#expireNow()} fires (e.g.
 * {@code UserAdminService} revoked the principal), the next request lands here.
 *
 * <p>For API-shaped requests the strategy delegates to {@link ApiAuthenticationEntryPoint} so the
 * SPA's fetch wrapper sees a JSON 401 envelope (matching the entry-point and access-denied
 * dispatch). For browser navigation it issues a redirect to {@code /login}; the form-login flow
 * then takes over.
 */
public class PathAwareSessionExpiredStrategy implements SessionInformationExpiredStrategy {

    private final ApiAuthenticationEntryPoint apiEntryPoint;
    private final String loginPath;

    public PathAwareSessionExpiredStrategy(ApiAuthenticationEntryPoint apiEntryPoint, String loginPath) {
        this.apiEntryPoint = apiEntryPoint;
        this.loginPath = loginPath;
    }

    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event) throws IOException, ServletException {
        if (ApiRequestPaths.isApiRequest(event.getRequest())) {
            apiEntryPoint.commence(
                    event.getRequest(),
                    event.getResponse(),
                    new CredentialsExpiredException("Session expired or revoked"));
            return;
        }
        event.getResponse().sendRedirect(loginPath);
    }
}
