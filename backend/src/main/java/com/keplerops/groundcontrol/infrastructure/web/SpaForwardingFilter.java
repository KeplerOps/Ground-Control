package com.keplerops.groundcontrol.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Forwards non-API, non-static GET requests to {@code index.html} so that React Router can handle
 * client-side routing at any depth.
 *
 * <p>Replaces the previous depth-limited {@code SpaController} that enumerated path segments
 * S1–S6. A filter-based approach is inherently route-depth-agnostic.
 */
@Component
public class SpaForwardingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if ("GET".equals(request.getMethod()) && shouldForwardToSpa(path)) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    static boolean shouldForwardToSpa(String path) {
        if (path == null || path.equals("/") || path.isEmpty()) {
            return false;
        }

        if (path.startsWith("/api") || path.startsWith("/actuator")) {
            return false;
        }

        // Static assets have a file extension in the last segment
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return !lastSegment.contains(".");
    }
}
