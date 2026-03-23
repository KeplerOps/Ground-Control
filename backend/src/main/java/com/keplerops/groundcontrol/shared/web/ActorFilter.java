package com.keplerops.groundcontrol.shared.web;

import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates ActorHolder and MDC with the actor identity from X-Actor header.
 * Defaults to "anonymous" if the header is missing, blank, or contains unsafe characters.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ActorFilter extends OncePerRequestFilter {

    private static final String ACTOR_HEADER = "X-Actor";
    private static final String MDC_ACTOR = "actor";
    private static final String DEFAULT_ACTOR = "anonymous";
    private static final int MAX_ACTOR_LENGTH = 100;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String actor = sanitizeActor(request.getHeader(ACTOR_HEADER));
        ActorHolder.set(actor);
        MDC.put(MDC_ACTOR, actor);
        try {
            filterChain.doFilter(request, response);
        } finally {
            ActorHolder.clear();
            MDC.remove(MDC_ACTOR);
        }
    }

    /** Sanitize the actor header to prevent log injection and enforce reasonable length. */
    private static String sanitizeActor(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_ACTOR;
        }
        if (raw.length() > MAX_ACTOR_LENGTH) {
            return DEFAULT_ACTOR;
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return DEFAULT_ACTOR;
            }
        }
        return raw;
    }
}
