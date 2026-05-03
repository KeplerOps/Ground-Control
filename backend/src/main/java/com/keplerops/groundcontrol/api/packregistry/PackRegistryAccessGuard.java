package com.keplerops.groundcontrol.api.packregistry;

import com.keplerops.groundcontrol.domain.exception.AuthenticationException;
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
 */
@Component
@SuppressWarnings("java:S125") // JML block comments (/*@ ... @*/) are contracts, not commented-out code
public class PackRegistryAccessGuard {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    /*@ ensures \result != null && !\result.isBlank(); @*/
    public String requireAdminActor() {
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
