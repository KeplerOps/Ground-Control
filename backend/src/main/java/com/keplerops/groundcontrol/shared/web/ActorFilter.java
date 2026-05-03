package com.keplerops.groundcontrol.shared.web;

import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.shared.security.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates {@link ActorHolder} and the SLF4J MDC with the calling actor's identity for the
 * lifetime of the request.
 *
 * <p>Resolution order:
 * <ol>
 *   <li><b>Security enabled (production):</b> the authenticated principal from
 *       {@link SecurityContextHolder} when present, else {@code "anonymous"}. The
 *       {@code X-Actor} header is intentionally ignored — accepting it on permitted anonymous
 *       routes (e.g. {@code /actuator/health}) would let any caller spoof an audit identity.</li>
 *   <li><b>Security disabled (dev/test profile):</b> {@code X-Actor} header is honored as a
 *       legacy convenience, falling back to {@code "anonymous"}.</li>
 * </ol>
 *
 * <p>Runs after {@code AuthorizationFilter} so the security chain has had a chance to populate
 * the context. Order is set high enough to still wrap controllers.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@SuppressWarnings("java:S125") // JML block comments (/*@ ... @*/) are contracts, not commented-out code
public class ActorFilter extends OncePerRequestFilter {

    private static final String ACTOR_HEADER = "X-Actor";
    private static final String MDC_ACTOR = "actor";
    private static final String DEFAULT_ACTOR = "anonymous";

    private final SecurityProperties securityProperties;

    /**
     * Constructor takes an {@link ObjectProvider} so {@code @WebMvcTest} slices that don't
     * import the configuration-properties bean still wire ActorFilter (with a security-disabled
     * default that mirrors the {@code test} profile). Production runs always have the bean
     * available and the provider hands it back.
     */
    public ActorFilter(ObjectProvider<SecurityProperties> securityPropertiesProvider) {
        this.securityProperties = securityPropertiesProvider.getIfAvailable(SecurityProperties::new);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String actor = resolveActor(request);
        ActorHolder.set(actor);
        MDC.put(MDC_ACTOR, actor);
        try {
            filterChain.doFilter(request, response);
        } finally {
            ActorHolder.clear();
            MDC.remove(MDC_ACTOR);
        }
    }

    /*@ requires request != null;
    @ ensures \result != null && !\result.isBlank();
    @ pure @*/
    private String resolveActor(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && auth.getName() != null
                && !auth.getName().isBlank()) {
            return auth.getName();
        }
        if (!securityProperties.isEnabled()) {
            String header = request.getHeader(ACTOR_HEADER);
            if (header != null && !header.isBlank()) {
                return header;
            }
        }
        return DEFAULT_ACTOR;
    }
}
