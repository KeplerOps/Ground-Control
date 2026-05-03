package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.domain.exception.AuthenticationException;
import com.keplerops.groundcontrol.shared.security.SecurityProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Returns the authenticated admin principal name for pack-registry audit fields.
 *
 * <p>The actual authentication and {@code ROLE_ADMIN} authorization are performed by
 * {@link com.keplerops.groundcontrol.shared.security.ApiSecurityConfig} earlier in the filter
 * chain. This guard exists only to bridge from Spring Security's {@link SecurityContextHolder} to
 * the controller's audit identity, and to fail closed if security has been bypassed (for example
 * in dev profile) without a valid admin principal in scope.
 *
 * <p>When {@code groundcontrol.security.enabled=false} (dev/test profile), the security chain
 * does not populate the SecurityContext. The guard then falls back to {@link ActorHolder}, which
 * {@code ActorFilter} populates from the {@code X-Actor} request header (or {@code "anonymous"}
 * if absent). This keeps the local pack registry usable without forcing operators to mint admin
 * tokens just to develop, while production (security enabled) still enforces ROLE_ADMIN.
 */
@Component
@SuppressWarnings("java:S125") // JML block comments (/*@ ... @*/) are contracts, not commented-out code
public class PackRegistryAccessGuard {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final SecurityProperties securityProperties;

    public PackRegistryAccessGuard(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /*@ ensures \result != null && !\result.isBlank(); @*/
    public String requireAdminActor() {
        if (!securityProperties.isEnabled()) {
            String actor = ActorHolder.get();
            return actor != null && !actor.isBlank() ? actor : "anonymous";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationException("Pack registry admin authentication is required");
        }
        String name = auth.getName();
        if (name == null || name.isBlank()) {
            throw new AuthenticationException("Pack registry admin authentication is required");
        }
        if (!hasAdminAuthority(auth)) {
            throw new AuthenticationException("Pack registry endpoints require the admin role");
        }
        return name;
    }

    /*@ requires auth != null;
    @ pure @*/
    private static boolean hasAdminAuthority(Authentication auth) {
        for (GrantedAuthority granted : auth.getAuthorities()) {
            if (ADMIN_AUTHORITY.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
