package com.keplerops.groundcontrol.shared.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Browser-chain {@link AccessDeniedHandler} that routes {@code /api/v1/**} 403s to the canonical
 * {@link ApiAccessDeniedHandler} (stable JSON {@code ErrorResponse} envelope) and lets other
 * paths fall through to a default handler so SPA navigation hits Spring's regular flow.
 *
 * <p>Without this dispatch, an authenticated browser user who lands on an admin-only SPA path
 * would see the framework's HTML 403 page instead of the JSON envelope the SPA expects from its
 * {@code /api/v1/**} XHR calls. The mirror exists exactly because ADR-026's path matrix is
 * shared across both chains.
 */
public class PathAwareAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiAccessDeniedHandler apiHandler;
    private final AccessDeniedHandler fallback;

    public PathAwareAccessDeniedHandler(ApiAccessDeniedHandler apiHandler, AccessDeniedHandler fallback) {
        this.apiHandler = apiHandler;
        this.fallback = fallback;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        // Use getRequestURI() instead of getServletPath() — servlet path is empty when the
        // dispatcher servlet is mapped at "/" (the Ground Control default), so the JSON
        // dispatch silently degrades to the fallback. URI is stable across mappings.
        if (ApiRequestPaths.isApiRequest(request)) {
            apiHandler.handle(request, response, accessDeniedException);
            return;
        }
        fallback.handle(request, response, accessDeniedException);
    }
}
