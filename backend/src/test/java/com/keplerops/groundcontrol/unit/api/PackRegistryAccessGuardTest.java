package com.keplerops.groundcontrol.unit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.api.packregistry.PackRegistryAccessGuard;
import com.keplerops.groundcontrol.domain.exception.AuthenticationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class PackRegistryAccessGuardTest {

    private final PackRegistryAccessGuard guard = new PackRegistryAccessGuard();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedAdminPrincipalName() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "pack-admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        var actor = guard.requireAdminActor();

        assertThat(actor).isEqualTo("pack-admin");
    }

    @Test
    void rejectsRequest_whenSecurityContextIsEmpty() {
        assertThatThrownBy(guard::requireAdminActor)
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("authentication");
    }

    @Test
    void rejectsRequest_whenAuthenticationIsNotAuthenticated() {
        var auth = new UsernamePasswordAuthenticationToken("nobody", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(guard::requireAdminActor).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void rejectsRequest_whenAuthenticatedButMissingAdminRole() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(guard::requireAdminActor)
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("admin");
    }

    @Test
    void rejectsRequest_whenPrincipalNameIsBlank() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "  ", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(guard::requireAdminActor).isInstanceOf(AuthenticationException.class);
    }
}
