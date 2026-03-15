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
 * Defaults to "anonymous" if the header is missing or blank.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ActorFilter extends OncePerRequestFilter {

    private static final String ACTOR_HEADER = "X-Actor";
    private static final String MDC_ACTOR = "actor";
    private static final String DEFAULT_ACTOR = "anonymous";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String actor = request.getHeader(ACTOR_HEADER);
        if (actor == null || actor.isBlank()) {
            actor = DEFAULT_ACTOR;
        }
        ActorHolder.set(actor);
        MDC.put(MDC_ACTOR, actor);
        try {
            filterChain.doFilter(request, response);
        } finally {
            ActorHolder.clear();
            MDC.remove(MDC_ACTOR);
        }
    }
}
