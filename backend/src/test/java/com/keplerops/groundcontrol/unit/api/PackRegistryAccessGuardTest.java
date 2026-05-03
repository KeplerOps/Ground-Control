package com.keplerops.groundcontrol.unit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.api.packregistry.PackRegistryAccessGuard;
import com.keplerops.groundcontrol.domain.audit.ActorHolder;
import com.keplerops.groundcontrol.domain.exception.AuthenticationException;
import com.keplerops.groundcontrol.shared.security.SecurityProperties;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class PackRegistryAccessGuardTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        ActorHolder.clear();
    }

    private static PackRegistryAccessGuard guardWithSecurityEnabled(boolean enabled) {
        var props = new SecurityProperties();
        props.setEnabled(enabled);
        return new PackRegistryAccessGuard(props);
    }

    @Nested
    class SecurityEnabled {

        @Test
        void returnsAuthenticatedAdminPrincipalName() {
            var auth = UsernamePasswordAuthenticationToken.authenticated(
                    "pack-admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            var guard = guardWithSecurityEnabled(true);

            assertThat(guard.requireAdminActor()).isEqualTo("pack-admin");
        }

        @Test
        void rejectsRequest_whenSecurityContextIsEmpty() {
            var guard = guardWithSecurityEnabled(true);

            assertThatThrownBy(guard::requireAdminActor)
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("authentication");
        }

        @Test
        void rejectsRequest_whenAuthenticationIsNotAuthenticated() {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken("nobody", null));
            var guard = guardWithSecurityEnabled(true);

            assertThatThrownBy(guard::requireAdminActor).isInstanceOf(AuthenticationException.class);
        }

        @Test
        void rejectsRequest_whenAuthenticatedButMissingAdminRole() {
            var auth = UsernamePasswordAuthenticationToken.authenticated(
                    "user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            var guard = guardWithSecurityEnabled(true);

            assertThatThrownBy(guard::requireAdminActor)
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("admin");
        }

        @Test
        void rejectsRequest_whenPrincipalNameIsBlank() {
            var auth = UsernamePasswordAuthenticationToken.authenticated(
                    "  ", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            var guard = guardWithSecurityEnabled(true);

            assertThatThrownBy(guard::requireAdminActor).isInstanceOf(AuthenticationException.class);
        }
    }

    @Nested
    class SecurityDisabled {

        @Test
        void returnsActorHolderValue_whenPresent() {
            ActorHolder.set("dev-user");
            var guard = guardWithSecurityEnabled(false);

            assertThat(guard.requireAdminActor()).isEqualTo("dev-user");
        }

        @Test
        void fallsBackToAnonymous_whenActorHolderIsEmpty() {
            var guard = guardWithSecurityEnabled(false);

            assertThat(guard.requireAdminActor()).isEqualTo("anonymous");
        }

        @Test
        void doesNotConsultSecurityContext() {
            // Even with an unauthorized SecurityContext, security-disabled mode must not throw.
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken("nobody", null));
            ActorHolder.set("dev-actor");
            var guard = guardWithSecurityEnabled(false);

            assertThat(guard.requireAdminActor()).isEqualTo("dev-actor");
        }
    }
}
